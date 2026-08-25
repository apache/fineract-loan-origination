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

package org.apache.fineract.los.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.domain.enums.RiskCategory;

/** Detailed staff view of a loan application including applicant profile and approval history. */
@Getter
@Builder
public class StaffApplicationDetailResponse {

  // Application fields
  private final String applicationRef;
  private final LoanApplicationStatus status;
  private final BigDecimal requestedAmount;
  private final String currency;
  private final String loanPurpose;
  private final Integer tenorMonths;
  private final LocalDateTime submittedAt;
  private final LocalDateTime updatedAt;

  // Applicant fields
  private final String applicantName;
  private final String nationalId;
  private final Long fineractClientId;
  private final BigDecimal monthlyIncome;
  private final String employmentStatus;
  private final Integer employmentDurationMonths;
  private final BigDecimal existingLoanObligations;

  // ---- Credit score (null until UNDER_REVIEW) ----
  private final CreditScoreSummary creditScore;

  // ---- Approval stages (chronological) ----
  private final List<ApprovalStageSummary> approvalStages;

  // ---- Current stage (null when not UNDER_REVIEW) ----
  private final String currentApprovalStage;

  // ---- Fineract disbursement ----
  private final Long fineractLoanId;
  private final String fineractIntegrationStatus;
  private final LocalDateTime disbursedAt;

  // ---- Nested types ----

  @Getter
  @Builder
  public static class CreditScoreSummary {
    private final int score;
    private final RiskCategory riskRating;
    private final int incomeRatioScore;
    private final int debtBurdenScore;
    private final int employmentScore;
    private final int repaymentHistoryScore;
    private final int loanPurposeScore;
    private final LocalDateTime scoredAt;
  }

  @Getter
  @Builder
  public static class ApprovalStageSummary {
    private final String stage;
    private final String decision;
    private final String decidedBy;
    private final LocalDateTime decidedAt;
    private final String notes;
  }
}
