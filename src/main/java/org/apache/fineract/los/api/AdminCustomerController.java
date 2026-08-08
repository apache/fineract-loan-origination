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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.los.domain.CustomerCredential;
import org.apache.fineract.los.repository.CustomerCredentialRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Staff-only endpoint for registering customer portal accounts.
 *
 * <p>Called once per customer at onboarding time. Links a portal username/password to an existing
 * Fineract clientId so the customer can log in via POST /api/v1/auth/login.
 *
 * <p>Protected by the staff security chain — requires admin credentials.
 */
@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

  private final CustomerCredentialRepository credentialRepository;
  private final PasswordEncoder passwordEncoder;

  public record RegisterCustomerRequest(
      @NotBlank String username,
      @NotBlank String password,
      @NotNull @Positive Long fineractClientId,
      @NotBlank String tenantId) {}

  public record RegisterCustomerResponse(
      Long id, String username, Long fineractClientId, String tenantId) {}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public RegisterCustomerResponse register(
      @Valid @RequestBody final RegisterCustomerRequest request) {

    if (credentialRepository.existsByUsernameAndTenantId(request.username(), request.tenantId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Username already exists for this tenant");
    }

    final CustomerCredential saved =
        credentialRepository.save(
            new CustomerCredential(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.fineractClientId(),
                request.tenantId()));

    return new RegisterCustomerResponse(
        saved.getId(), saved.getUsername(), saved.getFineractClientId(), saved.getTenantId());
  }
}
