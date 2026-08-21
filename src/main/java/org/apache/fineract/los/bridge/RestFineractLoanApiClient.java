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

package org.apache.fineract.los.bridge;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateRequest;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateResponse;
import org.apache.fineract.los.bridge.dto.FineractLoanRequest;
import org.apache.fineract.los.bridge.dto.FineractLoanResponse;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Real {@link FineractLoanApiClient} implementation — calls Apache Fineract's loan API endpoints.
 * Uses a trust-all SSL context to support Fineract's self-signed certificate in development.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "los.fineract", name = "mock-enabled", havingValue = "false")
public class RestFineractLoanApiClient implements FineractLoanApiClient {

  private final FineractClientProperties properties;
  private final RestClient restClient;

  public RestFineractLoanApiClient(final FineractClientProperties properties) {
    this.properties = properties;

    this.restClient =
        RestClient.builder()
            .baseUrl(properties.getBaseUrl() + "/fineract-provider/api/v1")
            .requestFactory(trustAllRequestFactory())
            .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuthHeader(properties))
            .defaultHeader("X-Fineract-Platform-TenantId", properties.getTenantId())
            .build();

    log.info(
        "RestFineractLoanApiClient initialised against baseUrl={}/fineract-provider/api/v1 tenantId={}",
        properties.getBaseUrl(),
        properties.getTenantId());
  }

  /** Trust-all SSL factory — mirrors FineractRestTemplateConfig for self-signed dev certs. */
  private static HttpComponentsClientHttpRequestFactory trustAllRequestFactory() {
    try {
      final SSLContext sslContext =
          SSLContextBuilder.create().loadTrustMaterial((chain, authType) -> true).build();

      final SSLConnectionSocketFactory sslSocketFactory =
          new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

      final CloseableHttpClient httpClient =
          HttpClients.custom()
              .setConnectionManager(
                  PoolingHttpClientConnectionManagerBuilder.create()
                      .setSSLSocketFactory(sslSocketFactory)
                      .build())
              .build();

      return new HttpComponentsClientHttpRequestFactory(httpClient);
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Failed to create trust-all SSL context for Fineract RestClient", ex);
    }
  }

  @Override
  public FineractLoanCreateResponse createLoan(final FineractLoanCreateRequest request) {
    log.info(
        "Calling Fineract POST /loans: clientId={} productId={} principal={}",
        request.getClientId(),
        request.getProductId(),
        request.getPrincipal());

    try {
      return restClient
          .post()
          .uri("/loans")
          .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
          .body(request)
          .retrieve()
          .body(FineractLoanCreateResponse.class);
    } catch (RestClientException ex) {
      log.error("Failed to call Fineract POST /loans", ex);
      throw ex;
    }
  }

  @Override
  public FineractLoanResponse approveLoan(final Long loanId, final FineractLoanRequest request) {
    log.info(
        "Calling Fineract POST /loans/{}?command=approve: approvedOnDate={}",
        loanId,
        request.getApprovedOnDate());

    try {
      return restClient
          .post()
          .uri("/loans/{loanId}?command=approve", loanId)
          .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
          .body(request)
          .retrieve()
          .body(FineractLoanResponse.class);
    } catch (RestClientException ex) {
      log.error("Failed to call Fineract POST /loans/{}?command=approve", loanId, ex);
      throw ex;
    }
  }

  @Override
  public FineractLoanResponse disburseLoan(final Long loanId, final FineractLoanRequest request) {
    log.info(
        "Calling Fineract POST /loans/{}?command=disburse: actualDisbursementDate={}",
        loanId,
        request.getActualDisbursementDate());

    try {
      return restClient
          .post()
          .uri("/loans/{loanId}?command=disburse", loanId)
          .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
          .body(request)
          .retrieve()
          .body(FineractLoanResponse.class);
    } catch (RestClientException ex) {
      log.error("Failed to call Fineract POST /loans/{}?command=disburse", loanId, ex);
      throw ex;
    }
  }

  private String basicAuthHeader(final FineractClientProperties properties) {
    final String credentials = properties.getUsername() + ":" + properties.getPassword();

    return "Basic "
        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }
}
