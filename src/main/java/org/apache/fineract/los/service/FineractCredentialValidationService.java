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
package org.apache.fineract.los.service;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.api.dto.response.FineractAuthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class FineractCredentialValidationService {

  private final RestTemplate fineractRestTemplate;

  @Value("${los.fineract.base-url}")
  private String fineractBaseUrl;

  @Value("${los.fineract.tenant-id}")
  private String tenantId;

  public FineractAuthResponse validate(String username, String password) {

    try {

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("Fineract-Platform-TenantId", tenantId);

      Map<String, String> body = new HashMap<>();
      body.put("username", username);
      body.put("password", password);

      HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

      ResponseEntity<FineractAuthResponse> response =
          fineractRestTemplate.exchange(
              fineractBaseUrl + "/fineract-provider/api/v1/authentication",
              HttpMethod.POST,
              request,
              FineractAuthResponse.class);

      if (response.getStatusCode().is2xxSuccessful()
          && response.getBody() != null
          && response.getBody().isAuthenticated()) {

        log.info("Fineract auth OK for user={} roles={}", username, response.getBody().getRoles());
        return response.getBody();
      }

    } catch (HttpClientErrorException.Unauthorized e) {

      log.warn(
          "Fineract rejected credentials for user: {} - Status: {}", username, e.getStatusCode());

    } catch (Exception e) {

      log.error("Fineract auth call failed for user: {} - Error: {}", username, e.getMessage(), e);
    }

    return null;
  }
}
