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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

  public PasswordValidationResult validate(
      final String password, final String username, final String email) {
    if (password == null || password.isBlank()) {
      return new PasswordValidationResult(List.of(PasswordValidationError.MISSING_PASSWORD));
    }

    List<PasswordValidationError> errors = new ArrayList<>();

    if (password.codePointCount(0, password.length()) < 12) {
      errors.add(PasswordValidationError.TOO_SHORT);
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
