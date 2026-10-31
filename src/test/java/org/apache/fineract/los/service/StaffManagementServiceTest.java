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

package org.apache.fineract.los.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.fineract.los.domain.StaffCredential;
import org.apache.fineract.los.exception.WeakPasswordException;
import org.apache.fineract.los.repository.StaffCredentialRepository;
import org.apache.fineract.los.validation.PasswordValidationError;
import org.apache.fineract.los.validation.PasswordValidationResult;
import org.apache.fineract.los.validation.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class StaffManagementServiceTest {

  @Mock private StaffCredentialRepository repository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private PasswordValidator passwordValidator;

  private StaffManagementService service;

  @BeforeEach
  void setUp() {
    service = new StaffManagementService(repository, passwordEncoder, passwordValidator);
  }

  @Test
  void validPasswordAccepted() {
    final String password = "a-valid-password";

    when(passwordValidator.validate(password, "alice", "alice@example.com"))
        .thenReturn(new PasswordValidationResult(Collections.emptyList()));

    when(passwordEncoder.encode(password)).thenReturn("encoded-password");

    when(repository.save(any(StaffCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    final StaffCredential result =
        service.createStaff("alice", password, "alice@example.com", "ROLE_STAFF", "default");

    assertThat(result.getUsername()).isEqualTo("alice");
    assertThat(result.getPasswordHash()).isEqualTo("encoded-password");
    assertThat(result.getEmail()).isEqualTo("alice@example.com");
    assertThat(result.getRole()).isEqualTo("ROLE_STAFF");
    assertThat(result.getTenantId()).isEqualTo("default");

    verify(passwordValidator).validate(password, "alice", "alice@example.com");
    verify(passwordEncoder).encode(password);
    verify(repository).save(result);
  }

  @Test
  void weakPasswordRejectedBeforeHashingAndSaving() {
    final List<PasswordValidationError> errors = List.of(PasswordValidationError.TOO_SHORT);

    when(passwordValidator.validate("short", "alice", "alice@example.com"))
        .thenReturn(new PasswordValidationResult(errors));

    assertThatThrownBy(
            () ->
                service.createStaff("alice", "short", "alice@example.com", "ROLE_STAFF", "default"))
        .isInstanceOfSatisfying(
            WeakPasswordException.class,
            ex -> assertThat(ex.getErrors()).containsExactlyElementsOf(errors));

    verify(passwordValidator).validate("short", "alice", "alice@example.com");

    verifyNoInteractions(passwordEncoder, repository);
  }

  @ParameterizedTest
  @CsvSource(
      value = {"NULL, alice@example.com", "new@example.com, new@example.com"},
      nullValues = "NULL")
  void validatesPasswordAgainstEffectiveEmail(
      final String requestedEmail, final String expectedEmail) {
    final String password = "a-valid-password";
    final StaffCredential staffCredential =
        new StaffCredential("alice", "password-hash", "alice@example.com", "ROLE_STAFF", "default");

    when(passwordValidator.validate(password, "alice", expectedEmail))
        .thenReturn(new PasswordValidationResult(Collections.emptyList()));

    when(passwordEncoder.encode(password)).thenReturn("encoded-password");

    when(repository.findById(1L)).thenReturn(Optional.of(staffCredential));

    when(repository.save(any(StaffCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.updateStaff(1L, requestedEmail, null, password);

    verify(passwordValidator).validate(password, "alice", expectedEmail);
    verify(passwordEncoder).encode(password);
    verify(repository).save(staffCredential);
    assertThat(staffCredential.getPasswordHash()).isEqualTo("encoded-password");
    assertThat(staffCredential.getEmail()).isEqualTo(expectedEmail);
  }

  @Test
  void weakPasswordRejectedForUpdate() {
    final String password = "short";
    final StaffCredential staffCredential =
        new StaffCredential("alice", "password-hash", "alice@example.com", "ROLE_STAFF", "default");

    when(repository.findById(1L)).thenReturn(Optional.of(staffCredential));

    when(passwordValidator.validate(password, "alice", "new@example.com"))
        .thenReturn(new PasswordValidationResult(List.of(PasswordValidationError.TOO_SHORT)));

    assertThatThrownBy(() -> service.updateStaff(1L, "new@example.com", "ROLE_ADMIN", password))
        .isInstanceOfSatisfying(
            WeakPasswordException.class,
            ex -> assertThat(ex.getErrors()).containsExactly(PasswordValidationError.TOO_SHORT));

    verify(repository, never()).save(any(StaffCredential.class));
    verifyNoInteractions(passwordEncoder);
    assertThat(staffCredential.getPasswordHash()).isEqualTo("password-hash");
    assertThat(staffCredential.getEmail()).isEqualTo("alice@example.com");
    assertThat(staffCredential.getRole()).isEqualTo("ROLE_STAFF");
  }

  @Test
  void fieldsUpdatedWithPasswordNull() {
    final StaffCredential staffCredential =
        new StaffCredential("alice", "password-hash", "alice@example.com", "ROLE_STAFF", "default");

    when(repository.findById(1L)).thenReturn(Optional.of(staffCredential));

    when(repository.save(any(StaffCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    StaffCredential result = service.updateStaff(1L, "new-alice@example.com", "ROLE_ADMIN", null);

    assertThat(result.getEmail()).isEqualTo("new-alice@example.com");
    assertThat(result.getRole()).isEqualTo("ROLE_ADMIN");
    verifyNoInteractions(passwordValidator, passwordEncoder);
    verify(repository).save(staffCredential);

    assertThat(result.getPasswordHash()).isEqualTo("password-hash");
  }
}
