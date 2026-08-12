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

package org.apache.fineract.los.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Startup guard that prevents the application from booting outside of dev/test profiles when the
 * JWT signing secret is still set to one of the known-insecure defaults shipped with the codebase.
 *
 * <p>This bean is only registered when the active Spring profile is neither {@code dev} nor {@code
 * test}. In those profiles a weak secret is intentional and expected.
 *
 * <p>If the secret is insecure the application will fail to start with a clear error message
 * instructing the operator to set the {@code JWT_SECRET} environment variable.
 */
@Component
@Profile("!dev & !test")
@RequiredArgsConstructor
public class JwtSecretValidator {

  private final JwtProperties jwtProperties;

  @PostConstruct
  public void validate() {
    if (jwtProperties.isKnownInsecureDefault()) {
      throw new IllegalStateException(
          "JWT signing secret is set to a known-insecure default value. "
              + "Set the JWT_SECRET environment variable to a strong, unique secret before "
              + "starting the application outside of dev/test profiles.");
    }
    if (jwtProperties.getSecret().length() < 32) {
      throw new IllegalStateException(
          "JWT signing secret must be at least 32 characters. "
              + "Set the JWT_SECRET environment variable to a sufficiently long secret.");
    }
  }
}
