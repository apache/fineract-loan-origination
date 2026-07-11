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

package org.apache.fineract.los.bridge.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/**
 * Exact payload structure expected by Fineract's
 * {@code POST /fineract-provider/api/v1/loans} endpoint.
 *
 * <p>Field names match Fineract's JSON contract exactly —
 * do not rename without verifying against the Fineract API.
 */
@Getter
@Builder
public class FineractLoanRequest {

    /** Fineract client ID for the borrower. */
    private final Long clientId;

    /** Fineract loan product ID. */
    private final Long productId;

    /** Loan amount — matches LoanApplication.requestedAmount. */
    private final BigDecimal principal;

    /** Repayment frequency — e.g. 12 for 12 months. */
    private final Integer loanTermFrequency;

    /**
     * Repayment frequency type.
     * 0=days, 1=weeks, 2=months, 3=years.
     */
    private final Integer loanTermFrequencyType;

    /** Loan type — "individual" or "group". */
    private final String loanType;

    /** Interest rate per period. */
    private final BigDecimal interestRatePerPeriod;

    /** Amortization type. 1=equal installments. */
    private final Integer amortizationType;

    /** Number of repayments. */
    private final Integer numberOfRepayments;

    /** Repayment every N periods. */
    private final Integer repaymentEvery;

    /** Expected disbursement date. Format: dd MMMM yyyy. */
    private final String expectedDisbursementDate;

    /** Submission date. Format: dd MMMM yyyy. */
    private final String submittedOnDate;

    /** Loan purpose ID in Fineract. Optional. */
    private final Long loanPurposeId;
}