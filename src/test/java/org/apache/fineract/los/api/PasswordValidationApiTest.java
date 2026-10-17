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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.apache.fineract.los.exception.GlobalExceptionHandler;
import org.apache.fineract.los.exception.LosErrorConstants;
import org.apache.fineract.los.repository.CustomerCredentialRepository;
import org.apache.fineract.los.repository.StaffCredentialRepository;
import org.apache.fineract.los.service.StaffManagementService;
import org.apache.fineract.los.validation.PasswordValidationError;
import org.apache.fineract.los.validation.PasswordValidationResult;
import org.apache.fineract.los.validation.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class PasswordValidationApiTest {

  @Mock private CustomerCredentialRepository customerCredentialRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private PasswordValidator passwordValidator;
  @Mock private StaffCredentialRepository staffCredentialRepository;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    final AdminCustomerController adminCustomerController =
        new AdminCustomerController(
            customerCredentialRepository, passwordEncoder, passwordValidator);

    final StaffManagementService staffManagementService =
        new StaffManagementService(staffCredentialRepository, passwordEncoder, passwordValidator);

    final AdminStaffController adminStaffController =
        new AdminStaffController(staffManagementService);

    mockMvc =
        MockMvcBuilders.standaloneSetup(adminCustomerController, adminStaffController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("Returns validation details for a weak customer password")
  void weakCustomerPasswordRejected() throws Exception {
    when(passwordValidator.validate("short", "alice"))
        .thenReturn(
            new PasswordValidationResult(
                List.of(
                    PasswordValidationError.TOO_SHORT, PasswordValidationError.COMMON_PASSWORD)));

    final String request =
        """
                {
                  "username": "alice",
                  "password": "short",
                  "fineractClientId": 1,
                  "tenantId": "default"
                }
                """;

    final MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/admin/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(LosErrorConstants.MSG_WEAK_PASSWORD))
            .andExpect(
                jsonPath(
                    "$.details",
                    containsInAnyOrder(
                        LosErrorConstants.MSG_PASSWORD_TOO_SHORT,
                        LosErrorConstants.MSG_PASSWORD_TOO_COMMON)))
            .andReturn();

    assertThat(result.getResponse().getContentAsString()).doesNotContain("short");

    verify(passwordValidator).validate("short", "alice");
    verifyNoInteractions(passwordEncoder, customerCredentialRepository);
  }

  @Test
  @DisplayName("Returns validation details for an empty customer password")
  void emptyCustomerPasswordRejected() throws Exception {
    final String request =
        """
                {
                  "username": "alice",
                  "password": "",
                  "fineractClientId": 1,
                  "tenantId": "default"
                }
                """;

    mockMvc
        .perform(
            post("/api/v1/admin/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(LosErrorConstants.MSG_REQUEST_VALIDATION_FAILED))
        .andExpect(jsonPath("$.details", contains(startsWith("password: "))));

    verifyNoInteractions(passwordValidator, passwordEncoder, customerCredentialRepository);
  }

  @Test
  @DisplayName("Rejects malformed customer JSON without exposing the password")
  void invalidCustomerRequestRejected(final CapturedOutput output) throws Exception {
    final String request =
        """
                    {
                      "username": "alice",
                      "password": just-a-password,
                      "fineractClientId": 1,
                      "tenantId": "default"
                    }
                    """;

    final MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/admin/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(LosErrorConstants.MSG_REQUEST_BODY_UNREADABLE))
            .andReturn();

    assertThat(result.getResponse().getContentAsString()).doesNotContain("just-a-password");

    verifyNoInteractions(passwordValidator, passwordEncoder, customerCredentialRepository);
    assertThat(output.getAll()).doesNotContain("just-a-password");
  }

  @Test
  @DisplayName("Returns validation details for a weak staff password")
  void weakStaffPasswordRejected() throws Exception {
    when(passwordValidator.validate("short", "alice", "alice@example.com"))
        .thenReturn(
            new PasswordValidationResult(
                List.of(
                    PasswordValidationError.TOO_SHORT, PasswordValidationError.COMMON_PASSWORD)));

    final String request =
        """
                {
                  "username": "alice",
                  "password": "short",
                  "email": "alice@example.com",
                  "role": "ROLE_STAFF",
                  "tenantId": "default"
                }
                """;

    final MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/admin/staff")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(LosErrorConstants.MSG_WEAK_PASSWORD))
            .andExpect(
                jsonPath(
                    "$.details",
                    containsInAnyOrder(
                        LosErrorConstants.MSG_PASSWORD_TOO_SHORT,
                        LosErrorConstants.MSG_PASSWORD_TOO_COMMON)))
            .andReturn();

    assertThat(result.getResponse().getContentAsString()).doesNotContain("short");

    verify(passwordValidator).validate("short", "alice", "alice@example.com");
    verifyNoInteractions(passwordEncoder, staffCredentialRepository);
  }

  @Test
  @DisplayName("Returns validation details for an empty staff password")
  void emptyStaffPasswordRejected() throws Exception {
    final String request =
        """
                {
                  "username": "alice",
                  "password": "",
                  "email": "alice@example.com",
                  "role": "ROLE_STAFF",
                  "tenantId": "default"
                }
                """;

    mockMvc
        .perform(
            post("/api/v1/admin/staff").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(LosErrorConstants.MSG_REQUEST_VALIDATION_FAILED))
        .andExpect(jsonPath("$.details", contains(startsWith("password: "))));

    verifyNoInteractions(passwordValidator, passwordEncoder, staffCredentialRepository);
  }

  @Test
  @DisplayName("Rejects malformed staff JSON without exposing the password")
  void invalidStaffRequestRejected(final CapturedOutput output) throws Exception {
    final String request =
        """
                    {
                      "username": "alice",
                      "password": just-a-password,
                      "email": "alice@example.com",
                      "role": "ROLE_STAFF",
                      "tenantId": "default"
                    }
                    """;

    final MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/admin/staff")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(LosErrorConstants.MSG_REQUEST_BODY_UNREADABLE))
            .andReturn();

    assertThat(result.getResponse().getContentAsString()).doesNotContain("just-a-password");

    verifyNoInteractions(passwordValidator, passwordEncoder, staffCredentialRepository);
    assertThat(output.getAll()).doesNotContain("just-a-password");
  }
}
