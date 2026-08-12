/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.los.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.domain.CustomerCredential;
import org.apache.fineract.los.infrastructure.fineract.FineractAuthResponse;
import org.apache.fineract.los.repository.CustomerCredentialRepository;
import org.apache.fineract.los.security.JwtService;
import org.apache.fineract.los.service.FineractCredentialValidationService;
import org.apache.fineract.los.workflow.ApprovalWorkflowProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Authentication endpoints for customers and staff.
 *
 * <p>Customer login: validates credentials against the local {@code customer_credentials} table
 * using BCrypt; issues a LOS JWT with {@code userType=CUSTOMER}.
 *
 * <p>Staff login: delegates credential validation to Fineract ({@code POST
 * /fineract-provider/api/v1/authentication}). On success the Fineract role names returned in the
 * response are mapped to LOS workflow roles via {@link
 * ApprovalWorkflowProperties#stageForFineractRole(String)}, and a LOS JWT is issued with {@code
 * userType=STAFF} and the resolved {@code role} (e.g. {@code ROLE_LOAN_OFFICER}). No staff
 * passwords are ever stored in LOS.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final CustomerCredentialRepository credentialRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final FineractCredentialValidationService fineractValidationService;
  private final ApprovalWorkflowProperties workflowProperties;

  // -------------------------------------------------------------------------
  // Request / Response records
  // -------------------------------------------------------------------------

  public record LoginRequest(
      @NotBlank String username, @NotBlank String password, @NotBlank String tenantId) {}

  public record LoginResponse(
      String token,
      String username,
      Long clientId,
      String tenantId,
      String role,
      String userType,
      int expiresInMinutes) {}

  public record StaffLoginRequest(
      @NotBlank String username, @NotBlank String password, @NotBlank String tenantId) {}

  public record StaffLoginResponse(
      String token,
      String username,
      String losRole,
      String displayRole,
      String tenantId,
      String userType,
      int expiresInMinutes) {}

  // -------------------------------------------------------------------------
  // Customer login
  // -------------------------------------------------------------------------

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody final LoginRequest request) {
    final Optional<CustomerCredential> credentialOpt =
        credentialRepository.findByUsername(request.username());

    if (credentialOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    final CustomerCredential credential = credentialOpt.get();

    if (!credential.getTenantId().equals(request.tenantId())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    final String token =
        jwtService.generateToken(
            credential.getUsername(),
            credential.getFineractClientId(),
            credential.getTenantId(),
            "ROLE_CUSTOMER",
            "CUSTOMER");

    return ResponseEntity.ok(
        new LoginResponse(
            token,
            credential.getUsername(),
            credential.getFineractClientId(),
            credential.getTenantId(),
            "ROLE_CUSTOMER",
            "CUSTOMER",
            15));
  }

  // -------------------------------------------------------------------------
  // Staff login (Fineract-delegated)
  // -------------------------------------------------------------------------

  /**
   * Authenticates a staff member against Fineract and issues a LOS JWT.
   *
   * <p>Flow:
   *
   * <ol>
   *   <li>POST the credentials to Fineract {@code /api/v1/authentication}.
   *   <li>Fineract returns its granted permissions/roles for this user.
   *   <li>We look for any permission that maps to an LOS workflow stage via {@code
   *       los.workflow.role-mapping} (e.g. {@code loan_officer -> LOAN_OFFICER}).
   *   <li>Issue a JWT with {@code role=ROLE_<LOS_STAGE>} and {@code userType=STAFF}.
   * </ol>
   *
   * <p>If Fineract cannot authenticate the user, or the user has no mapped LOS role, a 401 is
   * returned.
   */
  @PostMapping("/staff/login")
  public StaffLoginResponse staffLogin(@Valid @RequestBody final StaffLoginRequest request) {

    // Step 1: validate against Fineract
    final FineractAuthResponse fineractResponse =
        fineractValidationService.validate(request.username(), request.password());

    if (fineractResponse == null || !fineractResponse.isAuthenticated()) {
      log.warn(
          "Staff login failed (Fineract rejected credentials): username={}", request.username());
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    // Step 2: resolve the LOS workflow role from Fineract permissions
    // Fineract roles come back in permissions as role names like "loan_officer"
    final String losStage = resolvelosStage(fineractResponse);
    if (losStage == null) {
      log.warn(
          "Staff login failed (no LOS role mapping found): username={} roles={}",
          request.username(),
          fineractResponse.getRoles());
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Your Fineract account has no LOS workflow role assigned. "
              + "Ask an admin to assign loan_officer, credit_committee, or branch_manager.");
    }

    // Step 3: issue LOS JWT — role stored as ROLE_<STAGE> e.g. ROLE_LOAN_OFFICER
    final String jwtRole = "ROLE_" + losStage;
    final String token =
        jwtService.generateToken(request.username(), null, request.tenantId(), jwtRole, "STAFF");

    log.info(
        "Staff login successful: username={} losRole={} tenantId={}",
        request.username(),
        jwtRole,
        request.tenantId());

    return new StaffLoginResponse(
        token, request.username(), jwtRole, displayName(losStage), request.tenantId(), "STAFF", 15);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  /**
   * Resolves the LOS stage from the Fineract auth response.
   *
   * <p>Fineract returns the user's assigned roles in a {@code roles} array, where each entry has a
   * {@code name} field (e.g. "loan_officer"). We check those names against the configured {@code
   * los.workflow.role-mapping}. The raw {@code permissions} array contains action-level permission
   * codes like "CREATE_CLIENT" — those are never role names.
   */
  private String resolvelosStage(final FineractAuthResponse fineractResponse) {
    // Primary: check role names (e.g. "loan_officer", "credit_committee", "branch_manager")
    if (fineractResponse.getRoles() != null) {
      for (final FineractAuthResponse.FineractRole role : fineractResponse.getRoles()) {
        if (role.getName() == null) continue;
        final String key = role.getName().toLowerCase();
        final String stage = workflowProperties.stageForFineractRole(key);
        log.info("Role mapping check: fineractRole='{}' -> losStage='{}'", key, stage);
        if (stage != null) return stage;
      }
    }
    log.warn(
        "No LOS stage found. roleMapping keys={}", workflowProperties.getRoleMapping().keySet());
    return null;
  }

  /** Returns a human-readable display name for a LOS stage identifier. */
  private String displayName(final String losStage) {
    return switch (losStage) {
      case "LOAN_OFFICER" -> "Loan Officer";
      case "CREDIT_COMMITTEE" -> "Credit Committee";
      case "BRANCH_MANAGER" -> "Branch Manager";
      default -> losStage;
    };
  }
}
