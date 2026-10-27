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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

  private final Set<String> commonPasswords;

  public PasswordValidator() {
    final InputStream stream = PasswordValidator.class.getResourceAsStream("/common-passwords.txt");

    if (stream == null) {
      throw new IllegalStateException("Password blocklist resource is missing");
    }

    final Set<String> passwords = createPasswords(stream);

    if (passwords.isEmpty()) {
      throw new IllegalStateException("Password blocklist is empty");
    }

    this.commonPasswords = Set.copyOf(passwords);
  }

  PasswordValidator(final Set<String> commonPasswords) {
    this.commonPasswords = Set.copyOf(commonPasswords);
  }

  private static Set<String> createPasswords(final InputStream stream) {
    final Set<String> passwords = new HashSet<>();

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.isEmpty()) {
          passwords.add(line);
        }
      }
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to load password blocklist", ex);
    }
    return passwords;
  }

  public PasswordValidationResult validate(
      final String password, final String username, final String email) {
    if (password == null || password.isBlank()) {
      return new PasswordValidationResult(List.of(PasswordValidationError.MISSING_PASSWORD));
    }

    List<PasswordValidationError> errors = new ArrayList<>();

    if (password.codePointCount(0, password.length()) < 12) {
      errors.add(PasswordValidationError.TOO_SHORT);
    }

    if (this.commonPasswords.contains(password)) {
      errors.add(PasswordValidationError.COMMON_PASSWORD);
    }

    final String normalizedPassword = password.toLowerCase(Locale.ROOT);

    if (username != null && !username.isBlank()) {
      if (normalizedPassword.contains(username.toLowerCase(Locale.ROOT))) {
        errors.add(PasswordValidationError.CONTAINS_USERNAME);
      }
    }

    if (email != null && !email.isBlank()) {
      if (normalizedPassword.contains(email.toLowerCase(Locale.ROOT))) {
        errors.add(PasswordValidationError.CONTAINS_EMAIL);
      }
    }

    return new PasswordValidationResult(errors);
  }

  public PasswordValidationResult validate(final String password, final String username) {
    return validate(password, username, null);
  }
}
