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
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@RequiredArgsConstructor
public class JwtAuthFilter implements jakarta.servlet.Filter {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String CLAIM_CLIENT_ID = "clientId";
  private static final String CLAIM_TENANT_ID = "tenantId";
  private static final String CLAIM_CORRELATION_ID = "correlationId";
  private static final String CLAIM_USER_TYPE = "userType";
  private static final String CLAIM_LOS_ROLE = "losRole";
  private static final String TENANT_HEADER = "X-Fineract-Platform-TenantId";

  private final JwtService jwtService;

  @Override
  public void doFilter(
      final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {

    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      chain.doFilter(request, response);
      return;
    }

    try {
      final String token = authHeader.substring(BEARER_PREFIX.length());
      final Claims claims = jwtService.validateAndExtract(token);

      final String username = claims.getSubject();
      final String jwtTenantId = claims.get(CLAIM_TENANT_ID, String.class);
      final String correlationId = claims.get(CLAIM_CORRELATION_ID, String.class);
      final String userType = claims.get(CLAIM_USER_TYPE, String.class);

      // SECURITY: Validate JWT tenantId matches request header to prevent cross-tenant access
      final String headerTenantId = httpRequest.getHeader(TENANT_HEADER);
      if (jwtTenantId != null && headerTenantId != null && !jwtTenantId.equals(headerTenantId)) {
        SecurityContextHolder.clearContext();
        ((HttpServletResponse) response)
            .sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Tenant mismatch: JWT tenant ["
                    + jwtTenantId
                    + "] does not match request header ["
                    + headerTenantId
                    + "]");
        return;
      }

      // Add correlation and tenant to MDC for logging
      if (correlationId != null) {
        MDC.put("correlationId", correlationId);
      }
      if (jwtTenantId != null) {
        MDC.put("tenantId", jwtTenantId);
      }

      final UsernamePasswordAuthenticationToken authentication;

      // Create appropriate principal based on user type
      if ("STAFF".equals(userType)) {
        // Staff user - extract LOS role
        final String losRole = claims.get(CLAIM_LOS_ROLE, String.class);
        final StaffPrincipal staffPrincipal = new StaffPrincipal(username, jwtTenantId, losRole);
        authentication =
            UsernamePasswordAuthenticationToken.authenticated(
                staffPrincipal, null, staffPrincipal.getAuthorities());
        MDC.put("userType", "STAFF");
        MDC.put("losRole", losRole != null ? losRole : "UNKNOWN");
      } else {
        // Customer user - extract client ID
        final Number clientIdNum = claims.get(CLAIM_CLIENT_ID, Number.class);
        final Long clientId = clientIdNum != null ? clientIdNum.longValue() : null;
        final CustomerPrincipal customerPrincipal =
            new CustomerPrincipal(username, null, clientId, jwtTenantId, username);
        authentication =
            UsernamePasswordAuthenticationToken.authenticated(
                customerPrincipal, null, customerPrincipal.getAuthorities());
        MDC.put("userType", "CUSTOMER");
        if (clientId != null) {
          MDC.put("clientId", String.valueOf(clientId));
        }
      }

      SecurityContextHolder.getContext().setAuthentication(authentication);

      chain.doFilter(request, response);
    } catch (JwtService.InvalidJwtException e) {
      SecurityContextHolder.clearContext();
      ((HttpServletResponse) response)
          .sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
    } finally {
      MDC.remove("correlationId");
      MDC.remove("tenantId");
      MDC.remove("userType");
      MDC.remove("losRole");
      MDC.remove("clientId");
    }
  }
}
