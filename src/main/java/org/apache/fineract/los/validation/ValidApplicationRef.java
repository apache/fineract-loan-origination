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

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation constraint for loan application reference format.
 *
 * <p>Enforces the pattern: LOS-{YEAR}-{SEQUENCE} where:
 *
 * <ul>
 *   <li>YEAR is a 4-digit year (e.g., 2026)
 *   <li>SEQUENCE is a 5-digit zero-padded number (e.g., 00001)
 * </ul>
 *
 * <p>Examples of valid references:
 *
 * <ul>
 *   <li>LOS-2026-00001
 *   <li>LOS-2025-12345
 *   <li>LOS-2024-99999
 * </ul>
 *
 * <p>This validation prevents:
 *
 * <ul>
 *   <li>Path traversal attacks (e.g., ../../../etc/passwd)
 *   <li>SQL injection attempts (e.g., '; DROP TABLE)
 *   <li>XSS payloads (e.g., &lt;script&gt;alert(1)&lt;/script&gt;)
 *   <li>Malformed references causing unexpected errors
 * </ul>
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {})
@Pattern(
    regexp = "^LOS-\\d{4}-\\d{5}$",
    message = "applicationRef must follow format LOS-YYYY-NNNNN (e.g., LOS-2026-00001)")
public @interface ValidApplicationRef {

  String message() default
      "applicationRef must follow format LOS-YYYY-NNNNN (e.g., LOS-2026-00001)";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
