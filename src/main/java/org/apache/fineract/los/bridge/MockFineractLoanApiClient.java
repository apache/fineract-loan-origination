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

import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateRequest;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock {@link FineractLoanApiClient} implementation — simulates {@code POST /loans} without calling
 * a real Fineract instance.
 *
 * <p>Active by default ({@code los.fineract.mock-enabled=true}), per the proposal's risk mitigation
 * for FINERACT-2418 being unavailable during the coding period: "the integration layer will use
 * mocked or simulated Fineract responses so that all origination flows can be fully demonstrated
 * without dependency on unreleased APIs."
 *
 * <p>Generates a monotonically increasing fake {@code loanId} per JVM instance — sufficient for
 * demonstrating the end-to-end DRAFT → ... → DISBURSED flow in local development, integration
 * tests, and CI, without a running Fineract dependency.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "los.fineract",
    name = "mock-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class MockFineractLoanApiClient implements FineractLoanApiClient {

  private static final long MOCK_OFFICE_ID = 1L;
  private static final long MOCK_ID_SEED = 100000L;

  private final AtomicLong sequence = new AtomicLong(MOCK_ID_SEED);

  @Override
  public FineractLoanCreateResponse createLoan(final FineractLoanCreateRequest request) {
    final long fakeLoanId = sequence.incrementAndGet();

    log.warn(
        "FINERACT-2418 not available — returning MOCK Fineract loan creation "
            + "response. clientId={} productId={} principal={} mockLoanId={}",
        request.getClientId(),
        request.getProductId(),
        request.getPrincipal(),
        fakeLoanId);

    return FineractLoanCreateResponse.builder()
        .officeId(MOCK_OFFICE_ID)
        .clientId(request.getClientId())
        .loanId(fakeLoanId)
        .resourceId(fakeLoanId)
        .build();
  }
}
