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

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.bridge.FineractClientProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Authenticates customers by delegating to Fineract's POST /v1/authentication endpoint.
 *
 * <p>Spring Security calls {@link #loadUserByUsername(String)} with the username from the Basic
 * auth header. We forward the credentials to Fineract, which validates them and returns the linked
 * {@code clientId}. That clientId is what scopes all subsequent data access.
 *
 * <p>The password is passed via a thread-local because Spring's {@link UserDetailsService} contract
 * only provides the username. The actual credential check happens inside Fineract — Spring
 * Security's password comparison is bypassed by returning a {@link NoOpCredential}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FineractUserDetailsService implements UserDetailsService {

  static final ThreadLocal<String> PASSWORD_HOLDER = new ThreadLocal<>();

  private final RestTemplate restTemplate;
  private final FineractClientProperties fineractProps;

  @Override
  public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
    final String password = PASSWORD_HOLDER.get();
    if (password == null) {
      throw new UsernameNotFoundException("No password available for: " + username);
    }

    final FineractAuthResponse response = callFineract(username, password);

    if (!response.isAuthenticated()) {
      throw new UsernameNotFoundException("Fineract rejected credentials for: " + username);
    }

    if (response.getClientId() == null) {
      throw new UsernameNotFoundException(
          "User ["
              + username
              + "] is not linked to a Fineract client. "
              + "Only app users linked to a client record may access the customer portal.");
    }

    final String displayName =
        response.getDisplayName() != null ? response.getDisplayName() : username;

    return new CustomerPrincipal(
        username, NoOpCredential.VALUE, response.getClientId(), displayName);
  }

  private FineractAuthResponse callFineract(final String username, final String password) {
    final String url = fineractProps.getBaseUrl() + "/authentication";

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Fineract-Platform-TenantId", fineractProps.getTenantId());

    final HttpEntity<Map<String, String>> request =
        new HttpEntity<>(Map.of("username", username, "password", password), headers);

    try {
      final FineractAuthResponse response =
          restTemplate.postForObject(url, request, FineractAuthResponse.class);
      if (response == null) {
        throw new UsernameNotFoundException("Empty response from Fineract for: " + username);
      }
      return response;
    } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.BadRequest e) {
      log.debug("Fineract rejected credentials for user: {}", username);
      throw new UsernameNotFoundException("Invalid credentials for: " + username);
    } catch (Exception e) {
      log.error("Fineract auth call failed for user {}: {}", username, e.getMessage());
      throw new UsernameNotFoundException("Fineract unavailable, cannot authenticate: " + username);
    }
  }

  /** Sentinel password value — actual validation is done by Fineract, not Spring Security. */
  static final class NoOpCredential {
    static final String VALUE = "__fineract_validated__";
  }
}
