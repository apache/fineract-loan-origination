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

import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "los.jwt")
public class JwtProperties {

  /**
   * Known-insecure default secrets that must never reach a non-dev environment. Any value on this
   * list causes a hard startup failure outside of dev/test profiles.
   */
  private static final Set<String> KNOWN_INSECURE_DEFAULTS =
      Set.of(
          "change-me-in-production-min-32-chars!!", "los-dev-secret-min-32-chars-change-in-prod!!");

  /** HMAC-SHA256 signing secret — must be at least 32 characters. */
  private String secret = "change-me-in-production-min-32-chars!!";

  /** Token validity in minutes. Defaults to 15. */
  private int expiryMinutes = 15;

  /**
   * Returns {@code true} if the configured secret is one of the known-insecure defaults shipped
   * with the codebase.
   *
   * <p>Called from {@link JwtSecretValidator} at startup to fail fast when a default secret would
   * otherwise silently reach a non-dev environment.
   */
  public boolean isKnownInsecureDefault() {
    return KNOWN_INSECURE_DEFAULTS.contains(secret);
  }
}
