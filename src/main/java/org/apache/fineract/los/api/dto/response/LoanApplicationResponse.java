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
import lombok.Builder;
import lombok.Getter;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;

/**
 * API-facing view of a {@link LoanApplication}.
 *
 * <p>Deliberately decoupled from the JPA entity so the wire format doesn't silently change every
 * time the schema does.
 */
@Getter
@Builder
public class LoanApplicationResponse {

  private final String applicationRef;
  private final LoanApplicationStatus status;
  private final BigDecimal requestedAmount;
  private final String currency;
  private final String loanPurpose;
  private final Integer tenorMonths;
  private final Long fineractLoanProductId;
  private final Long fineractLoanId;
  private final Long fineractClientId;
  private final String applicantName;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  // ── Referral information ──
  /** Most recent referral comments from staff, if status is REFERRED. Null otherwise. */
  private final String referralComments;

  /** Most recent referral stage name, if status is REFERRED. Null otherwise. */
  private final String referralStage;

  // ── Current approval stage (for staff filtering) ──
  /** Current approval stage awaiting decision, if status is UNDER_REVIEW. Null otherwise. */
  private final String currentApprovalStage;

  public static LoanApplicationResponse from(final LoanApplication app) {
    return LoanApplicationResponse.builder()
        .applicationRef(app.getApplicationRef())
        .status(app.getStatus())
        .requestedAmount(app.getRequestedAmount())
        .currency(app.getCurrency())
        .loanPurpose(app.getLoanPurpose())
        .tenorMonths(app.getTenorMonths())
        .fineractLoanProductId(app.getFineractLoanProductId())
        .fineractLoanId(app.getFineractLoanId())
        .fineractClientId(app.getFineractClientId())
        .applicantName(null)
        .createdAt(app.getCreatedAt())
        .updatedAt(app.getUpdatedAt())
        .referralComments(null)
        .referralStage(null)
        .currentApprovalStage(null)
        .build();
  }

  /**
   * Creates response with applicant name from profile.
   *
   * @param app the loan application
   * @param applicantName the applicant's full name from ApplicantProfile
   * @return response DTO
   */
  public static LoanApplicationResponse from(
      final LoanApplication app, final String applicantName) {
    return LoanApplicationResponse.builder()
        .applicationRef(app.getApplicationRef())
        .status(app.getStatus())
        .requestedAmount(app.getRequestedAmount())
        .currency(app.getCurrency())
        .loanPurpose(app.getLoanPurpose())
        .tenorMonths(app.getTenorMonths())
        .fineractLoanProductId(app.getFineractLoanProductId())
        .fineractLoanId(app.getFineractLoanId())
        .fineractClientId(app.getFineractClientId())
        .applicantName(applicantName)
        .createdAt(app.getCreatedAt())
        .updatedAt(app.getUpdatedAt())
        .referralComments(null)
        .referralStage(null)
        .currentApprovalStage(null)
        .build();
  }

  /**
   * Creates response with applicant name and referral information.
   *
   * @param app the loan application
   * @param applicantName the applicant's full name from ApplicantProfile
   * @param referralComments staff comments explaining referral, null if not referred
   * @param referralStage workflow stage that referred the application, null if not referred
   * @return response DTO
   */
  public static LoanApplicationResponse from(
      final LoanApplication app,
      final String applicantName,
      final String referralComments,
      final String referralStage) {
    return LoanApplicationResponse.builder()
        .applicationRef(app.getApplicationRef())
        .status(app.getStatus())
        .requestedAmount(app.getRequestedAmount())
        .currency(app.getCurrency())
        .loanPurpose(app.getLoanPurpose())
        .tenorMonths(app.getTenorMonths())
        .fineractLoanProductId(app.getFineractLoanProductId())
        .fineractLoanId(app.getFineractLoanId())
        .fineractClientId(app.getFineractClientId())
        .applicantName(applicantName)
        .createdAt(app.getCreatedAt())
        .updatedAt(app.getUpdatedAt())
        .referralComments(referralComments)
        .referralStage(referralStage)
        .currentApprovalStage(null)
        .build();
  }

  /**
   * Creates response with all information including current approval stage for staff filtering.
   *
   * @param app the loan application
   * @param applicantName the applicant's full name from ApplicantProfile
   * @param referralComments staff comments explaining referral, null if not referred
   * @param referralStage workflow stage that referred the application, null if not referred
   * @param currentApprovalStage current stage awaiting decision, null if not UNDER_REVIEW
   * @return response DTO
   */
  public static LoanApplicationResponse from(
      final LoanApplication app,
      final String applicantName,
      final String referralComments,
      final String referralStage,
      final String currentApprovalStage) {
    return LoanApplicationResponse.builder()
        .applicationRef(app.getApplicationRef())
        .status(app.getStatus())
        .requestedAmount(app.getRequestedAmount())
        .currency(app.getCurrency())
        .loanPurpose(app.getLoanPurpose())
        .tenorMonths(app.getTenorMonths())
        .fineractLoanProductId(app.getFineractLoanProductId())
        .fineractLoanId(app.getFineractLoanId())
        .fineractClientId(app.getFineractClientId())
        .applicantName(applicantName)
        .createdAt(app.getCreatedAt())
        .updatedAt(app.getUpdatedAt())
        .referralComments(referralComments)
        .referralStage(referralStage)
        .currentApprovalStage(currentApprovalStage)
        .build();
  }
}
