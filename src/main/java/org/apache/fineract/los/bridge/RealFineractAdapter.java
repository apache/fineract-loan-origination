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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.bridge.dto.FineractLoanRequest;
import org.apache.fineract.los.bridge.dto.FineractLoanResponse;
import org.apache.fineract.los.config.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Real implementation of {@link FineractIntegrationPort}.
 *
 * <p>Active on {@code prod} Spring profile only. Calls
 * Fineract's {@code POST /loans} API with the exact payload
 * structure Fineract expects.
 *
 * <p>The {@code X-Correlation-Id} header is forwarded on
 * every outbound call so that LOS logs and Fineract core
 * logs can be joined on the same identifier during debugging
 * (FINERACT-1656).
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class RealFineractAdapter implements FineractIntegrationPort {

    private final RestTemplate restTemplate;

    @Value("${los.fineract.base-url}")
    private String fineractBaseUrl;

    @Value("${los.fineract.tenant-id:default}")
    private String fineractTenantId;

    @Value("${los.fineract.username}")
    private String fineractUsername;

    @Value("${los.fineract.password}")
    private String fineractPassword;

    @Override
    public FineractLoanResponse createLoan(
            final FineractLoanRequest request) {

        final String url = fineractBaseUrl
                + "/fineract-provider/api/v1/loans";

        final HttpHeaders headers = buildHeaders();
        final HttpEntity<FineractLoanRequest> entity =
                new HttpEntity<>(request, headers);

        log.info(
                "Calling Fineract POST /loans: "
                        + "principal={} clientId={}",
                request.getPrincipal(),
                request.getClientId());

        final FineractLoanResponse response =
                restTemplate.postForObject(
                        url, entity, FineractLoanResponse.class);

        log.info(
                "Fineract loan created: loanId={}",
                response != null ? response.getLoanId() : "null");

        return response;
    }

    /**
     * Builds HTTP headers for the Fineract API call including:
     * - Basic auth credentials
     * - Tenant ID header
     * - Correlation ID forwarded from MDC
     */
    private HttpHeaders buildHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(fineractUsername, fineractPassword);
        headers.set("Fineract-Platform-TenantId", fineractTenantId);

        final String correlationId = MDC.get(
                CorrelationIdFilter.MDC_KEY);
        if (StringUtils.hasText(correlationId)) {
            headers.set(
                    CorrelationIdFilter.CORRELATION_ID_HEADER,
                    correlationId);
        }

        return headers;
    }
}