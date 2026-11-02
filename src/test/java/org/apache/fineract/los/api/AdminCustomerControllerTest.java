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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.apache.fineract.los.domain.CustomerCredential;
import org.apache.fineract.los.exception.WeakPasswordException;
import org.apache.fineract.los.repository.CustomerCredentialRepository;
import org.apache.fineract.los.validation.PasswordValidationError;
import org.apache.fineract.los.validation.PasswordValidationResult;
import org.apache.fineract.los.validation.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminCustomerControllerTest {

  @Mock private CustomerCredentialRepository repository;

  @Mock private PasswordValidator passwordValidator;

  @Mock private PasswordEncoder passwordEncoder;

  private AdminCustomerController controller;

  @BeforeEach
  void setUp() {
    controller = new AdminCustomerController(repository, passwordEncoder, passwordValidator);
  }

  @Test
  void weakPasswordRejectedBeforeHashingAndSaving() {
    final AdminCustomerController.RegisterCustomerRequest request =
        new AdminCustomerController.RegisterCustomerRequest("user-name", "short", 1L, "default");
    final List<PasswordValidationError> errors = List.of(PasswordValidationError.TOO_SHORT);

    when(passwordValidator.validate("short", "user-name"))
        .thenReturn(new PasswordValidationResult(errors));

    assertThatThrownBy(() -> controller.register(request))
        .isInstanceOfSatisfying(
            WeakPasswordException.class,
            ex -> assertThat(ex.getErrors()).containsExactlyElementsOf(errors));

    verify(passwordValidator).validate("short", "user-name");

    verifyNoInteractions(passwordEncoder, repository);
  }

  @Test
  void validPasswordAccepted() {
    final String password = "a-valid-password";
    final AdminCustomerController.RegisterCustomerRequest request =
        new AdminCustomerController.RegisterCustomerRequest("user-name", password, 1L, "default");

    when(passwordValidator.validate(password, "user-name"))
        .thenReturn(new PasswordValidationResult(Collections.emptyList()));

    when(passwordEncoder.encode(password)).thenReturn("encoded-password");

    when(repository.save(any(CustomerCredential.class)))
        .thenAnswer(
            invocation -> {
              final CustomerCredential customer = invocation.getArgument(0);
              customer.setId(42L);
              return customer;
            });

    final AdminCustomerController.RegisterCustomerResponse response = controller.register(request);

    final ArgumentCaptor<CustomerCredential> captor =
        ArgumentCaptor.forClass(CustomerCredential.class);

    verify(repository).save(captor.capture());

    final CustomerCredential savedCustomer = captor.getValue();

    assertThat(savedCustomer.getUsername()).isEqualTo(request.username());
    assertThat(savedCustomer.getPasswordHash()).isEqualTo("encoded-password");
    assertThat(savedCustomer.getFineractClientId()).isEqualTo(request.fineractClientId());
    assertThat(savedCustomer.getTenantId()).isEqualTo(request.tenantId());

    verify(passwordValidator).validate(password, request.username());
    verify(passwordEncoder).encode(password);

    assertThat(response.id()).isEqualTo(42L);
    assertThat(response.username()).isEqualTo(request.username());
    assertThat(response.fineractClientId()).isEqualTo(request.fineractClientId());
    assertThat(response.tenantId()).isEqualTo(request.tenantId());
  }
}
