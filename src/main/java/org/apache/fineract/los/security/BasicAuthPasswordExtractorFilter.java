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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.HttpHeaders;

/**
 * Extracts the raw password from the Basic auth header and stores it in {@link
 * FineractUserDetailsService#PASSWORD_HOLDER} so the UserDetailsService can forward it to Fineract
 * for validation.
 *
 * <p>The thread-local is always cleared in the finally block to prevent leaks.
 */
public class BasicAuthPasswordExtractorFilter implements Filter {

  @Override
  public void doFilter(
      final ServletRequest request, final ServletResponse response, final FilterChain filterChain)
      throws ServletException, IOException {

    try {
      final String authHeader = ((HttpServletRequest) request).getHeader(HttpHeaders.AUTHORIZATION);
      if (authHeader != null && authHeader.startsWith("Basic ")) {
        final String decoded =
            new String(Base64.getDecoder().decode(authHeader.substring(6)), StandardCharsets.UTF_8);
        final int colon = decoded.indexOf(':');
        if (colon > 0) {
          FineractUserDetailsService.PASSWORD_HOLDER.set(decoded.substring(colon + 1));
        }
      }
      filterChain.doFilter(request, response);
    } finally {
      FineractUserDetailsService.PASSWORD_HOLDER.remove();
    }
  }
}
