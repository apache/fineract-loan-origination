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
import org.apache.fineract.los.domain.ApplicantProfile;
import org.apache.fineract.los.domain.ApprovalStage;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;

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

  // Related data
  private final List<ApprovalStageDto> approvalStages;
  private final Long fineractLoanId;
  private final LocalDateTime disbursedAt;

  @Getter
  @Builder
  public static class ApprovalStageDto {
    private final String stage;
    private final String decision;
    private final String decidedBy;
    private final LocalDateTime decidedAt;
    private final String notes;
  }

  public static StaffApplicationDetailResponse from(
      final LoanApplication app,
      final ApplicantProfile profile,
      final List<ApprovalStage> approvalStages) {

    return StaffApplicationDetailResponse.builder()
        .applicationRef(app.getApplicationRef())
        .status(app.getStatus())
        .requestedAmount(app.getRequestedAmount())
        .currency(app.getCurrency())
        .loanPurpose(app.getLoanPurpose())
        .tenorMonths(app.getTenorMonths())
        .submittedAt(app.getCreatedAt())
        .updatedAt(app.getUpdatedAt())
        .applicantName(profile.getFullName())
        .nationalId(profile.getNationalId())
        .fineractClientId(profile.getFineractClientId())
        .monthlyIncome(profile.getMonthlyIncome())
        .employmentStatus(profile.getEmploymentStatus())
        .employmentDurationMonths(profile.getEmploymentDurationMonths())
        .existingLoanObligations(profile.getExistingLoanObligations())
        .approvalStages(
            approvalStages.stream()
                .map(
                    stage ->
                        ApprovalStageDto.builder()
                            .stage(stage.getStageName())
                            .decision(
                                stage.getDecision() != null ? stage.getDecision().name() : null)
                            .decidedBy(stage.getAssignedOfficer())
                            .decidedAt(stage.getDecidedAt())
                            .notes(stage.getComments())
                            .build())
                .toList())
        .fineractLoanId(app.getFineractLoanId())
        .disbursedAt(null) // TODO: Add disbursement timestamp when implemented
        .build();
  }
}
