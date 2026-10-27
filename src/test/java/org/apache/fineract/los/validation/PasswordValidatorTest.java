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

package org.apache.fineract.los.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Password Validation Tests")
public class PasswordValidatorTest {
  @Test
  @DisplayName("Rejects a password shorter than 12 characters")
  void tooShortPasswordRejected() {
    final PasswordValidator validator = new PasswordValidator(Set.of("common-password"));
    final PasswordValidationResult result = validator.validate("a".repeat(11), "test-username");

    assertThat(result.isValid()).isFalse();
    assertThat(result.errors()).containsExactly(PasswordValidationError.TOO_SHORT);
  }

  @Test
  @DisplayName("Rejects a common and short password")
  void commonAndShortPasswordRejected() {
    final PasswordValidator validator = new PasswordValidator(Set.of("short"));

    final PasswordValidationResult result = validator.validate("short", "user-name");

    assertThat(result.errors())
        .containsExactlyInAnyOrder(
            PasswordValidationError.TOO_SHORT, PasswordValidationError.COMMON_PASSWORD);
  }

  @Test
  @DisplayName("Accepts a password containing exactly 12 characters")
  void minLengthPasswordAccepted() {
    final PasswordValidator validator = new PasswordValidator(Set.of("common-password"));

    final PasswordValidationResult result = validator.validate("a".repeat(12), "test-username");

    assertThat(result.isValid()).isTrue();
    assertThat(result.errors()).isEmpty();
  }

  @Test
  @DisplayName("Rejects a password from blocklist")
  void blocklistPasswordRejected() {
    final PasswordValidator validator = new PasswordValidator(Set.of("a".repeat(15)));

    final PasswordValidationResult result = validator.validate("a".repeat(15), "test-username");

    assertThat(result.isValid()).isFalse();
    assertThat(result.errors()).containsExactly(PasswordValidationError.COMMON_PASSWORD);
  }

  @Test
  @DisplayName("Rejects a password from the bundled blocklist")
  void blocklistSourcePasswordRejected() {
    final PasswordValidator validator = new PasswordValidator();

    final PasswordValidationResult result = validator.validate("q1w2e3r4t5y6", "test-username");

    assertThat(result.isValid()).isFalse();
    assertThat(result.errors()).containsExactly(PasswordValidationError.COMMON_PASSWORD);
  }

  @Test
  @DisplayName("Rejects a password containing the username regardless of case")
  void passwordContainingUsernameIgnoringCaseRejected() {
    final PasswordValidator validator = new PasswordValidator(Set.of("common-password"));

    final PasswordValidationResult result =
        validator.validate("my-uSeR-NamE-like-PASSworD-pass", "USER-NAME-LIKE-PASSWORD");

    assertThat(result.isValid()).isFalse();
    assertThat(result.errors()).containsExactly(PasswordValidationError.CONTAINS_USERNAME);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  @DisplayName("Skips an absent or blank username")
  void absentUsernameSkipped(final String username) {
    final PasswordValidator validator = new PasswordValidator(Set.of("common-password"));

    final PasswordValidationResult result = validator.validate("a".repeat(15), username);

    assertThat(result.isValid()).isTrue();
    assertThat(result.errors()).isEmpty();
  }

  @Test
  @DisplayName("Rejects a password containing the email address regardless of case")
  void passwordContainingEmailIgnoringCaseRejected() {
    final PasswordValidator validator = new PasswordValidator(Set.of("common-password"));

    final PasswordValidationResult result =
        validator.validate("my-PASS-cont@Email.com-password", "user-name", "PASS-cONt@EmaiL.com");

    assertThat(result.isValid()).isFalse();
    assertThat(result.errors()).containsExactly(PasswordValidationError.CONTAINS_EMAIL);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  @DisplayName("Skips an absent or blank email")
  void absentEmailSkipped(final String email) {
    final PasswordValidator validator = new PasswordValidator(Set.of("common-password"));

    final PasswordValidationResult result = validator.validate("a".repeat(15), "user-name", email);

    assertThat(result.isValid()).isTrue();
    assertThat(result.errors()).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  @DisplayName("Rejects a missing or blank password")
  void missingPasswordRejected(final String password) {
    final PasswordValidator validator = new PasswordValidator(Set.of("common-password"));

    final PasswordValidationResult result = validator.validate(password, "user-name");

    assertThat(result.isValid()).isFalse();
    assertThat(result.errors()).containsExactly(PasswordValidationError.MISSING_PASSWORD);
  }

  @Test
  @DisplayName("Returns multiple validation errors")
  void returnsMultipleValidationErrors() {
    final PasswordValidator validator = new PasswordValidator(Set.of("common-password"));

    final PasswordValidationResult result =
        validator.validate("common-password", "CoMmOn", "PaSsWoRd");

    assertThat(result.isValid()).isFalse();
    assertThat(result.errors())
        .containsExactlyInAnyOrder(
            PasswordValidationError.COMMON_PASSWORD,
            PasswordValidationError.CONTAINS_USERNAME,
            PasswordValidationError.CONTAINS_EMAIL);
  }

  @Test
  @DisplayName("Accepts valid password")
  void validPasswordAccepted() {
    final PasswordValidator validator = new PasswordValidator(Set.of("common-password"));

    final PasswordValidationResult result =
        validator.validate("a".repeat(15), "user-name", "user-email");

    assertThat(result.isValid()).isTrue();
    assertThat(result.errors()).isEmpty();
  }

  @Test
  @DisplayName("Accepts a password containing exactly 12 Unicode code points")
  void acceptsPasswordWithTwelveUnicodeCodePoints() {
    final PasswordValidator validator = new PasswordValidator(Set.of("common-password"));

    final String character = "\uD840\uDC00"; // U+20000: 𠀀

    final PasswordValidationResult result = validator.validate(character.repeat(12), "user-name");

    assertThat(result.isValid()).isTrue();
    assertThat(result.errors()).isEmpty();
  }

  @Test
  @DisplayName("Rejects a password containing fewer than 12 Unicode code points")
  void rejectsPasswordWithElevenUnicodeCodePoints() {
    final PasswordValidator validator = new PasswordValidator(Set.of("common-password"));

    final String character = "\uD840\uDC00"; // U+20000: 𠀀

    final PasswordValidationResult result = validator.validate(character.repeat(11), "user-name");

    assertThat(result.isValid()).isFalse();
    assertThat(result.errors()).containsExactly(PasswordValidationError.TOO_SHORT);
  }
}
