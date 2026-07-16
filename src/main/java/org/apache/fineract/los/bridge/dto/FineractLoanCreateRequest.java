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
 * Request payload for Apache Fineract's {@code POST /loans} endpoint.
 *
 * <p>Field names deliberately match Fineract's own JSON API naming exactly (camelCase, Fineract
 * convention) since this object is serialised directly into the request body — see Fineract's
 * {@code LoanApplicationWritePlatformService} API docs for the authoritative field list. Only the
 * fields required to accept the loan are modelled here; institutions needing charges, collateral,
 * or guarantors at creation time can extend this DTO without touching {@code
 * DisbursementBridgeService}'s calling code.
 */
@Getter
@Builder
public class FineractLoanCreateRequest {

  /** Existing Fineract client this loan is created against. */
  private final Long clientId;

  /** Fineract loan product identifier. */
  private final Long productId;

  /** Principal amount requested. */
  private final BigDecimal principal;

  /** Loan term expressed in {@link #loanTermFrequencyType} units. */
  private final Integer loanTermFrequency;

  /** Fineract enum code for the loan term unit, e.g. "2" = Months. */
  private final String loanTermFrequencyType;

  /** Total number of repayment installments. */
  private final Integer numberOfRepayments;

  /** Number of {@link #repaymentFrequencyType} units between each repayment. */
  private final Integer repaymentEvery;

  /** Fineract enum code for the repayment frequency unit, e.g. "2" = Months. */
  private final String repaymentFrequencyType;

  /** Nominal interest rate per repayment period. */
  private final String interestRatePerPeriod;

  /** Fineract enum code identifying the repayment schedule processing strategy. */
  private final String transactionProcessingStrategyCode;

  /** Date the applicant expects disbursement, formatted per {@link #dateFormat}. */
  private final String expectedDisbursementDate;

  /** Submission date of the underlying Fineract loan record, formatted per {@link #dateFormat}. */
  private final String submittedOnDate;

  /** Fineract loanType — "individual" for the POC scope. */
  @Builder.Default private final String loanType = "individual";

  /** Fineract locale parameter, required on most write endpoints. */
  private final String locale;

  /** Fineract dateFormat parameter, required on most write endpoints. */
  private final String dateFormat;
}
