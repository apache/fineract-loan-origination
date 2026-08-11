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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.api.dto.response.FineractAuthResponse;
import org.apache.fineract.los.domain.CustomerCredential;
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

  public record LoginRequest(
      @NotBlank(message = "Username is required")
          @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
          String username,
      @NotBlank(message = "Password is required")
          @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
          String password,
      @NotBlank(message = "Tenant ID is required")
          @Pattern(
              regexp = "^[a-zA-Z0-9_-]{1,50}$",
              message = "Tenant ID must be alphanumeric with hyphens/underscores")
          String tenantId) {}

  public record LoginResponse(
      String token, String username, Long clientId, String tenantId, int expiresInMinutes) {}

  public record StaffLoginRequest(
      @NotBlank(message = "Username is required")
          @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
          String username,
      @NotBlank(message = "Password is required")
          @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
          String password,
      @NotBlank(message = "Tenant ID is required")
          @Pattern(
              regexp = "^[a-zA-Z0-9_-]{1,50}$",
              message = "Tenant ID must be alphanumeric with hyphens/underscores")
          String tenantId) {}

  public record StaffLoginResponse(
      String token,
      String username,
      String losRole,
      String displayRole,
      String tenantId,
      String userType,
      int expiresInMinutes) {}

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody final LoginRequest request) {
    final Optional<CustomerCredential> credentialOpt =
        credentialRepository.findByUsername(request.username());

    if (credentialOpt.isEmpty()) {
      log.warn("Customer login failed: username not found [{}]", request.username());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    final CustomerCredential credential = credentialOpt.get();

    if (!credential.getTenantId().equals(request.tenantId())) {
      log.warn(
          "Customer login failed: tenant mismatch for user [{}] - expected [{}], got [{}]",
          request.username(),
          credential.getTenantId(),
          request.tenantId());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
      log.warn("Customer login failed: invalid password for user [{}]", request.username());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    final String token =
        jwtService.generateToken(
            credential.getUsername(), credential.getFineractClientId(), credential.getTenantId());

    log.info(
        "Customer login successful: user [{}], clientId [{}]",
        request.username(),
        credential.getFineractClientId());

    return ResponseEntity.ok(
        new LoginResponse(
            token,
            credential.getUsername(),
            credential.getFineractClientId(),
            credential.getTenantId(),
            15));
  }

  /**
   * Staff login endpoint - validates credentials against Fineract and issues JWT with LOS role.
   *
   * <p>Security features:
   *
   * <ul>
   *   <li>Validates credentials against Fineract authentication API
   *   <li>Rejects users without Fineract roles mapped to LOS workflow stages
   *   <li>Issues JWT with userType=STAFF and losRole claim
   *   <li>Rate-limited by RateLimitFilter (5 requests per 15 minutes)
   *   <li>Input validation on username, password, and tenantId
   * </ul>
   *
   * @param request Staff login credentials
   * @return JWT token with staff role information
   */
  @PostMapping("/staff/login")
  public ResponseEntity<StaffLoginResponse> staffLogin(
      @Valid @RequestBody final StaffLoginRequest request) {

    log.debug(
        "Staff login attempt: username [{}], tenant [{}]", request.username(), request.tenantId());

    // Validate credentials against Fineract
    final FineractAuthResponse fineractResponse =
        fineractValidationService.validate(request.username(), request.password());

    if (fineractResponse == null || !fineractResponse.isAuthenticated()) {
      log.warn(
          "Staff login failed: Fineract rejected credentials for user [{}]", request.username());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    log.debug(
        "Fineract authentication successful for user [{}]. Roles: {}, Permissions: {}",
        request.username(),
        fineractResponse.getRoles(),
        fineractResponse.getPermissions());

    log.debug("Available role mappings: {}", workflowProperties.getRoleMapping());

    // Prevent customer users from logging in as staff
    if (fineractResponse.getClientId() != null) {
      log.warn(
          "Staff login failed: user [{}] is linked to clientId [{}] - customer users cannot use staff portal",
          request.username(),
          fineractResponse.getClientId());
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(
              new StaffLoginResponse(
                  null, request.username(), null, null, request.tenantId(), null, 0));
    }

    // Map Fineract roles to LOS workflow role
    String losRole = null;

    // First, try to map from role names
    if (fineractResponse.getRoles() != null && !fineractResponse.getRoles().isEmpty()) {
      for (FineractAuthResponse.FineractRole role : fineractResponse.getRoles()) {
        final String mappedStage = workflowProperties.stageForFineractRole(role.getName());
        if (mappedStage != null) {
          losRole = "ROLE_" + mappedStage;
          log.debug("Mapped Fineract role [{}] to LOS role [{}]", role.getName(), losRole);
          break;
        }
      }
    }

    // Fallback: try permissions if no role mapping found
    if (losRole == null
        && fineractResponse.getPermissions() != null
        && !fineractResponse.getPermissions().isEmpty()) {
      for (String permission : fineractResponse.getPermissions()) {
        final String mappedStage = workflowProperties.stageForFineractRole(permission);
        if (mappedStage != null) {
          losRole = "ROLE_" + mappedStage;
          log.debug("Mapped Fineract permission [{}] to LOS role [{}]", permission, losRole);
          break;
        }
      }
    }

    if (losRole == null) {
      log.warn(
          "Staff login failed: user [{}] has no LOS role mapping. Fineract roles: {}, permissions: {}",
          request.username(),
          fineractResponse.getRoles(),
          fineractResponse.getPermissions());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // Generate JWT token with staff claims
    final String token =
        jwtService.generateStaffToken(request.username(), request.tenantId(), losRole);

    // Convert role to display format (ROLE_LOAN_OFFICER -> Loan Officer)
    final String displayRole = formatDisplayRole(losRole);

    log.info("Staff login successful: user [{}], losRole [{}]", request.username(), losRole);

    return ResponseEntity.ok(
        new StaffLoginResponse(
            token, request.username(), losRole, displayRole, request.tenantId(), "STAFF", 15));
  }

  /** Convert ROLE_LOAN_OFFICER to "Loan Officer" for display. */
  private String formatDisplayRole(final String losRole) {
    if (losRole == null || !losRole.startsWith("ROLE_")) {
      return losRole;
    }
    final String roleWithoutPrefix = losRole.substring("ROLE_".length());
    // Convert LOAN_OFFICER to Loan Officer
    final String[] words = roleWithoutPrefix.split("_");
    final StringBuilder result = new StringBuilder();
    for (String word : words) {
      if (result.length() > 0) {
        result.append(" ");
      }
      result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
    }
    return result.toString();
  }
}
