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

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateRequest;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateResponse;
import org.apache.fineract.los.bridge.dto.FineractLoanRequest;
import org.apache.fineract.los.bridge.dto.FineractLoanResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Mock {@link FineractLoanApiClient} implementation — simulates the complete Fineract loan
 * lifecycle without calling a real Fineract instance.
 *
 * <p>Enabled only when {@code los.fineract.mock-enabled=true} in a dev or test profile.
 *
 * <p>Allocates fake {@code loanId} values from the shared PostgreSQL sequence, so allocations
 * remain unique across adapter instances and application restarts. This supports demonstrating the
 * complete loan lifecycle without a running Fineract dependency.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "los.fineract", name = "mock-enabled", havingValue = "true")
public class MockFineractLoanApiClient implements FineractLoanApiClient {

  private static final long MOCK_OFFICE_ID = 1L;
  private final JdbcTemplate jdbcTemplate;

  public MockFineractLoanApiClient(final JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @PostConstruct
  void logMockActivation() {
    log.warn(
        "MOCK FINERACT ADAPTER ACTIVE: no real Fineract operations will be performed; "
            + "loan IDs and lifecycle responses are simulated.");
  }

  @Override
  public FineractLoanCreateResponse createLoan(final FineractLoanCreateRequest request) {
    final Long fakeLoanId =
        jdbcTemplate.queryForObject("SELECT nextval('mock_fineract_loan_id_seq')", Long.class);

    log.warn(
        "MOCK Fineract loan creation response. clientId={} productId={} principal={} mockLoanId={}",
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

  @Override
  public FineractLoanResponse approveLoan(final Long loanId, final FineractLoanRequest request) {
    log.warn(
        "MOCK Fineract loan approval response. loanId={} approvedOnDate={}",
        loanId,
        request.getApprovedOnDate());

    return FineractLoanResponse.builder().loanId(loanId).resourceId(loanId).build();
  }

  @Override
  public FineractLoanResponse disburseLoan(final Long loanId, final FineractLoanRequest request) {
    log.warn(
        "MOCK Fineract loan disbursement response. loanId={} actualDisbursementDate={}",
        loanId,
        request.getActualDisbursementDate());

    return FineractLoanResponse.builder().loanId(loanId).resourceId(loanId).build();
  }
}
