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

package org.apache.fineract.los.scoring.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/**
 * Immutable input model for the credit scoring engine.
 *
 * Deliberately decoupled from JPA entities — the scoring engine must never depend on
 * persistence-layer classes. This allows scoring to be unit tested without any database or Spring
 * context.
 *
 * Built exclusively via the {@link Builder} — no public constructor or setters, preventing
 * partially-constructed instances from entering the scoring pipeline.
 */
@Getter
@Builder
public class ApplicantScoringProfile {

  /** Monthly income of the applicant in application currency. */
  private final BigDecimal monthlyIncome;

  /** Amount requested in the loan application. */
  private final BigDecimal requestedAmount;

  /** Total existing loan obligations per month. */
  private final BigDecimal existingLoanObligations;

  /** Employment status — EMPLOYED, SELF_EMPLOYED, etc. */
  private final String employmentStatus;

  /** Number of months in current employment. */
  private final Integer employmentDurationMonths;

  /**
   * Number of prior loans repaid successfully in Fineract. Null or zero indicates a first-time
   * borrower with no repayment history — handled explicitly, not penalised.
   */
  private final Integer successfulRepaymentsCount;

  /**
   * Number of prior loans with missed or late payments. Null or zero indicates no negative
   * repayment history.
   */
  private final Integer missedRepaymentsCount;

  /**
   * Purpose of the loan, e.g. AGRICULTURE, EDUCATION, BUSINESS, SPECULATION. Used to derive purpose
   * risk.
   */
  private final String loanPurpose;
}
