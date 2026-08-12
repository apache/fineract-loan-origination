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
import org.apache.fineract.los.bridge.dto.FineractLoanRequest;
import org.apache.fineract.los.bridge.dto.FineractLoanResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of {@link FineractIntegrationPort}.
 *
 * <p>Active on {@code dev} and {@code test} Spring profiles. Returns simulated loan IDs without
 * calling any external system — enables full end-to-end demonstration without a running Fineract
 * instance.
 *
 * <p>The generated loan ID starts at 100001 and increments per call, matching the format visible in
 * the database screenshots shared during midterm evaluation.
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class MockFineractAdapter implements FineractIntegrationPort {

  private static final AtomicLong LOAN_ID_SEQUENCE = new AtomicLong(100001);

  @Override
  public FineractLoanResponse createLoan(final FineractLoanRequest request) {

    final long simulatedLoanId = LOAN_ID_SEQUENCE.getAndIncrement();

    log.info(
        "[MOCK] Fineract loan created: " + "principal={} loanId={}",
        request.getPrincipal(),
        simulatedLoanId);

    return FineractLoanResponse.builder()
        .loanId(simulatedLoanId)
        .resourceId(simulatedLoanId)
        .build();
  }
}
