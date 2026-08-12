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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

  private static final String CLAIM_CLIENT_ID = "clientId";
  private static final String CLAIM_TENANT_ID = "tenantId";
  private static final String CLAIM_CORRELATION_ID = "correlationId";
  private static final String CLAIM_ROLE = "role";
  private static final String CLAIM_USER_TYPE = "userType";

  private final JwtProperties jwtProperties;

  public String generateToken(final String username, final Long clientId, final String tenantId) {
    return generateToken(username, clientId, tenantId, "ROLE_CUSTOMER", "CUSTOMER");
  }

  public String generateToken(
      final String username,
      final Long clientId,
      final String tenantId,
      final String role,
      final String userType) {

    final Date now = new Date();
    final Date expiry =
        new Date(now.getTime() + (long) jwtProperties.getExpiryMinutes() * 60 * 1000);

    return Jwts.builder()
        .subject(username)
        .claim(CLAIM_CLIENT_ID, clientId)
        .claim(CLAIM_TENANT_ID, tenantId)
        .claim(CLAIM_ROLE, role)
        .claim(CLAIM_USER_TYPE, userType)
        .claim(CLAIM_CORRELATION_ID, UUID.randomUUID().toString())
        .issuedAt(now)
        .expiration(expiry)
        .signWith(signingKey())
        .compact();
  }

  public Claims validateAndExtract(final String token) {
    try {
      return Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
    } catch (JwtException e) {
      throw new InvalidJwtException("Invalid or expired token", e);
    }
  }

  private SecretKey signingKey() {
    return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public static class InvalidJwtException extends RuntimeException {
    public InvalidJwtException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
