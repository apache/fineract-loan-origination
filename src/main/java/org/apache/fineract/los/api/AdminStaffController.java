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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.los.domain.StaffCredential;
import org.apache.fineract.los.service.StaffManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only endpoints for managing LOS staff accounts.
 *
 * <p>All endpoints require {@code ROLE_ADMIN}. Staff accounts created here can log in via {@code
 * POST /api/v1/auth/staff/login} and receive a LOS-issued JWT token.
 */
@Tag(
    name = "Admin — Staff Management",
    description = "Create, list, update, and deactivate staff accounts")
@RestController
@RequestMapping("/api/v1/admin/staff")
@RequiredArgsConstructor
public class AdminStaffController {

  private final StaffManagementService staffManagementService;

  // -------------------------------------------------------------------------
  // Request / Response records
  // -------------------------------------------------------------------------

  public record CreateStaffRequest(
      @NotBlank String username,
      @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
      @NotBlank String email,
      @NotBlank
          @Pattern(
              regexp = "ROLE_STAFF|ROLE_ADMIN",
              message = "role must be ROLE_STAFF or ROLE_ADMIN")
          String role,
      @NotBlank String tenantId) {}

  public record UpdateStaffRequest(
      String email,
      @Pattern(regexp = "ROLE_STAFF|ROLE_ADMIN", message = "role must be ROLE_STAFF or ROLE_ADMIN")
          String role,
      @Size(min = 8, message = "Password must be at least 8 characters") String password) {}

  public record StaffResponse(
      Long id,
      String username,
      String email,
      String role,
      String tenantId,
      boolean active,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    static StaffResponse from(final StaffCredential s) {
      return new StaffResponse(
          s.getId(),
          s.getUsername(),
          s.getEmail(),
          s.getRole(),
          s.getTenantId(),
          s.getActive(),
          s.getCreatedAt(),
          s.getUpdatedAt());
    }
  }

  // -------------------------------------------------------------------------
  // Endpoints
  // -------------------------------------------------------------------------

  @Operation(summary = "Create a new staff account")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public StaffResponse createStaff(@Valid @RequestBody final CreateStaffRequest request) {
    final StaffCredential staff =
        staffManagementService.createStaff(
            request.username(),
            request.password(),
            request.email(),
            request.role(),
            request.tenantId());
    return StaffResponse.from(staff);
  }

  @Operation(summary = "List all staff accounts for a tenant")
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<StaffResponse> listStaff(@RequestParam final String tenantId) {
    return staffManagementService.listStaff(tenantId).stream().map(StaffResponse::from).toList();
  }

  @Operation(summary = "Update a staff account's email, role, or password")
  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public StaffResponse updateStaff(
      @PathVariable final Long id, @Valid @RequestBody final UpdateStaffRequest request) {
    final StaffCredential updated =
        staffManagementService.updateStaff(id, request.email(), request.role(), request.password());
    return StaffResponse.from(updated);
  }

  @Operation(summary = "Deactivate a staff account (prevents login, preserves record)")
  @PostMapping("/{id}/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void deactivateStaff(@PathVariable final Long id) {
    staffManagementService.deactivateStaff(id);
  }

  @Operation(summary = "Reactivate a previously deactivated staff account")
  @PostMapping("/{id}/reactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void reactivateStaff(@PathVariable final Long id) {
    staffManagementService.reactivateStaff(id);
  }
}
