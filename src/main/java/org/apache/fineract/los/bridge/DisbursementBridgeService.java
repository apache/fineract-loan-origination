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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateRequest;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateResponse;
import org.apache.fineract.los.bridge.dto.FineractLoanRequest;
import org.apache.fineract.los.domain.ApplicantProfile;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.FineractIntegrationStatus;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.exception.ApplicantProfileNotFoundException;
import org.apache.fineract.los.exception.ApplicationNotFoundException;
import org.apache.fineract.los.exception.DisbursementNotAllowedException;
import org.apache.fineract.los.exception.FineractIntegrationException;
import org.apache.fineract.los.exception.LosErrorConstants;
import org.apache.fineract.los.repository.ApplicantProfileRepository;
import org.apache.fineract.los.repository.LoanApplicationRepository;
import org.apache.fineract.los.statemachine.LoanOriginationStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

/**
 * The disbursement bridge — the integration point between the external LOS and Apache Fineract's
 * existing loan module.
 *
 * <p>Orchestrates the complete Fineract loan lifecycle:
 *
 * <ol>
 *   <li>Create loan via POST /loans
 *   <li>Approve loan via POST /loans/{id}?command=approve
 *   <li>Disburse loan via POST /loans/{id}?command=disburse
 * </ol>
 *
 * <p>Only after successful disbursement does the LOS application transition to DISBURSED status.
 *
 * <p>Idempotency: uses {@code fineractIntegrationStatus} to track progress and prevent duplicate
 * operations. If a loan is already created, approval can be retried. If already approved,
 * disbursement can be retried. This prevents duplicate loan creation even if the orchestration is
 * called multiple times.
 *
 * <p>Transaction boundaries: external Fineract HTTP calls are isolated from database transactions
 * to avoid holding locks during network operations. Local state changes are persisted in separate
 * transactions after each successful Fineract operation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisbursementBridgeService {

  private static final DateTimeFormatter FINERACT_DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

  private final LoanApplicationRepository loanApplicationRepository;
  private final ApplicantProfileRepository applicantProfileRepository;
  private final FineractLoanApiClient fineractLoanApiClient;
  private final FineractClientProperties fineractClientProperties;
  private final LoanOriginationStateMachine stateMachine;

  /**
   * Orchestrates the complete Fineract loan lifecycle for an APPROVED LOS application.
   *
   * <p>This method is idempotent: it can be called multiple times and will resume from the last
   * successful state. If the loan is already created, it skips creation and proceeds to approval.
   * If already approved, it proceeds directly to disbursement.
   *
   * @param applicationRef human-readable application reference
   * @param tenantId institution identifier
   * @return the Fineract loan ID after successful disbursement
   */
  @Transactional
  public Long executeFullDisbursement(final String applicationRef, final String tenantId) {

    final LoanApplication application = loadApplication(applicationRef, tenantId);
    validateApproved(application);

    final ApplicantProfile profile = loadProfile(application, applicationRef, tenantId);
    validateFineractLinkage(application, profile);

    // Step 1: Create loan if not already created
    if (application.getFineractIntegrationStatus() == null) {
      createLoan(application, profile);
    } else {
      log.info(
          "Fineract loan already created for applicationRef={} with status={}, skipping creation",
          applicationRef,
          application.getFineractIntegrationStatus());
    }

    // Step 2: Approve loan if not already approved
    if (application.getFineractIntegrationStatus() == FineractIntegrationStatus.LOAN_CREATED) {
      approveLoan(application);
    } else if (application.getFineractIntegrationStatus() == FineractIntegrationStatus.LOAN_APPROVED
        || application.getFineractIntegrationStatus() == FineractIntegrationStatus.LOAN_DISBURSED) {
      log.info(
          "Fineract loan already approved for applicationRef={} with status={}, skipping approval",
          applicationRef,
          application.getFineractIntegrationStatus());
    }

    // Step 3: Disburse loan if not already disbursed
    if (application.getFineractIntegrationStatus() == FineractIntegrationStatus.LOAN_APPROVED) {
      disburseLoan(application);
    } else if (application.getFineractIntegrationStatus()
        == FineractIntegrationStatus.LOAN_DISBURSED) {
      log.info(
          "Fineract loan already disbursed for applicationRef={}, idempotent completion",
          applicationRef);
    }

    // Only after all three steps succeed does the LOS status become DISBURSED
    if (application.getStatus() != LoanApplicationStatus.DISBURSED) {
      stateMachine.transition(application, LoanApplicationStatus.DISBURSED);
      loanApplicationRepository.save(application);

      log.info(
          "LOS application transitioned to DISBURSED: applicationRef={} fineractLoanId={}",
          applicationRef,
          application.getFineractLoanId());
    }

    return application.getFineractLoanId();
  }

  /**
   * Step 1: Creates the Fineract loan via POST /loans.
   *
   * <p>Transaction boundary: Fineract HTTP call is made outside of any transaction, then local
   * state is persisted in a new transaction.
   */
  private void createLoan(final LoanApplication application, final ApplicantProfile profile) {
    final FineractLoanCreateRequest request = buildCreateRequest(application, profile);

    log.info(
        "Creating Fineract loan for applicationRef={} clientId={} productId={} principal={}",
        application.getApplicationRef(),
        request.getClientId(),
        request.getProductId(),
        request.getPrincipal());

    final FineractLoanCreateResponse response;
    try {
      response = fineractLoanApiClient.createLoan(request);
    } catch (final RestClientException ex) {
      log.error(
          "Fineract loan creation failed for applicationRef={}. Payload: clientId={} productId={} principal={} tenorMonths={} strategyCode={} error={}",
          application.getApplicationRef(),
          request.getClientId(),
          request.getProductId(),
          request.getPrincipal(),
          request.getLoanTermFrequency(),
          request.getTransactionProcessingStrategyCode(),
          ex.getMessage());
      application.setFineractIntegrationStatus(FineractIntegrationStatus.FAILED);
      loanApplicationRepository.save(application);
      throw new FineractIntegrationException(application.getApplicationRef(), ex);
    }

    application.setFineractLoanId(response.getLoanId());
    application.setFineractIntegrationStatus(FineractIntegrationStatus.LOAN_CREATED);
    loanApplicationRepository.save(application);

    log.info(
        "Fineract loan created: applicationRef={} fineractLoanId={}",
        application.getApplicationRef(),
        response.getLoanId());
  }

  /**
   * Step 2: Approves the Fineract loan via POST /loans/{id}?command=approve.
   *
   * <p>Requires the loan to already exist (fineractLoanId must be populated).
   */
  private void approveLoan(final LoanApplication application) {
    final Long loanId = application.getFineractLoanId();
    if (loanId == null) {
      throw new IllegalStateException(
          String.format(
              "Cannot approve Fineract loan for applicationRef=%s: fineractLoanId is null",
              application.getApplicationRef()));
    }

    final String today = LocalDate.now().format(FINERACT_DATE_FORMAT);
    final FineractLoanRequest request =
        FineractLoanRequest.builder()
            .approvedOnDate(today)
            .locale(fineractClientProperties.getLocale())
            .dateFormat(fineractClientProperties.getDateFormat())
            .build();

    log.info(
        "Approving Fineract loan: applicationRef={} fineractLoanId={} approvedOnDate={}",
        application.getApplicationRef(),
        loanId,
        today);

    try {
      fineractLoanApiClient.approveLoan(loanId, request);
    } catch (final RestClientException ex) {
      log.error(
          "Fineract loan approval failed for applicationRef={} fineractLoanId={}",
          application.getApplicationRef(),
          loanId,
          ex);
      application.setFineractIntegrationStatus(FineractIntegrationStatus.FAILED);
      loanApplicationRepository.save(application);
      throw new FineractIntegrationException(application.getApplicationRef(), ex);
    }

    application.setFineractIntegrationStatus(FineractIntegrationStatus.LOAN_APPROVED);
    loanApplicationRepository.save(application);

    log.info(
        "Fineract loan approved: applicationRef={} fineractLoanId={}",
        application.getApplicationRef(),
        loanId);
  }

  /**
   * Step 3: Disburses the Fineract loan via POST /loans/{id}?command=disburse.
   *
   * <p>Requires the loan to already be approved.
   */
  private void disburseLoan(final LoanApplication application) {
    final Long loanId = application.getFineractLoanId();
    if (loanId == null) {
      throw new IllegalStateException(
          String.format(
              "Cannot disburse Fineract loan for applicationRef=%s: fineractLoanId is null",
              application.getApplicationRef()));
    }

    final String today = LocalDate.now().format(FINERACT_DATE_FORMAT);
    final FineractLoanRequest request =
        FineractLoanRequest.builder()
            .actualDisbursementDate(today)
            .transactionAmount(application.getRequestedAmount())
            .locale(fineractClientProperties.getLocale())
            .dateFormat(fineractClientProperties.getDateFormat())
            .build();

    log.info(
        "Disbursing Fineract loan: applicationRef={} fineractLoanId={} amount={} date={}",
        application.getApplicationRef(),
        loanId,
        application.getRequestedAmount(),
        today);

    try {
      fineractLoanApiClient.disburseLoan(loanId, request);
    } catch (final RestClientException ex) {
      log.error(
          "Fineract loan disbursement failed for applicationRef={} fineractLoanId={}",
          application.getApplicationRef(),
          loanId,
          ex);
      application.setFineractIntegrationStatus(FineractIntegrationStatus.FAILED);
      loanApplicationRepository.save(application);
      throw new FineractIntegrationException(application.getApplicationRef(), ex);
    }

    application.setFineractIntegrationStatus(FineractIntegrationStatus.LOAN_DISBURSED);
    loanApplicationRepository.save(application);

    log.info(
        "Fineract loan disbursed: applicationRef={} fineractLoanId={}",
        application.getApplicationRef(),
        loanId);
  }

  private LoanApplication loadApplication(final String applicationRef, final String tenantId) {
    return loanApplicationRepository
        .findByApplicationRefAndTenantId(applicationRef, tenantId)
        .orElseThrow(() -> new ApplicationNotFoundException(applicationRef, tenantId));
  }

  private ApplicantProfile loadProfile(
      final LoanApplication application, final String applicationRef, final String tenantId) {
    return applicantProfileRepository
        .findByApplication(application)
        .orElseThrow(() -> new ApplicantProfileNotFoundException(applicationRef, tenantId));
  }

  private void validateApproved(final LoanApplication application) {
    if (application.getStatus() != LoanApplicationStatus.APPROVED) {
      throw new DisbursementNotAllowedException(
          String.format(
              LosErrorConstants.MSG_DISBURSEMENT_NOT_ALLOWED_TEMPLATE,
              application.getApplicationRef(),
              application.getStatus()));
    }
  }

  private void validateFineractLinkage(
      final LoanApplication application, final ApplicantProfile profile) {

    if (profile.getFineractClientId() == null) {
      throw new DisbursementNotAllowedException(
          String.format(
              LosErrorConstants.MSG_MISSING_FINERACT_CLIENT_ID_TEMPLATE,
              application.getApplicationRef()));
    }

    if (application.getFineractLoanProductId() == null) {
      throw new DisbursementNotAllowedException(
          String.format(
              LosErrorConstants.MSG_MISSING_LOAN_PRODUCT_TEMPLATE,
              application.getApplicationRef(),
              application.getLoanPurpose()));
    }
  }

  /**
   * Maps origination data collected during the LOS lifecycle onto Fineract's {@code POST /loans}
   * payload shape.
   */
  private FineractLoanCreateRequest buildCreateRequest(
      final LoanApplication application, final ApplicantProfile profile) {

    final String today = LocalDate.now().format(FINERACT_DATE_FORMAT);
    final Integer tenorMonths =
        application.getTenorMonths() == null ? 1 : application.getTenorMonths();

    return FineractLoanCreateRequest.builder()
        .clientId(profile.getFineractClientId())
        .productId(application.getFineractLoanProductId())
        .principal(application.getRequestedAmount())
        .loanTermFrequency(tenorMonths)
        .loanTermFrequencyType(fineractClientProperties.getDefaultFrequencyType())
        .numberOfRepayments(tenorMonths)
        .repaymentEvery(fineractClientProperties.getDefaultRepaymentEvery())
        .repaymentFrequencyType(fineractClientProperties.getDefaultFrequencyType())
        .interestRatePerPeriod(fineractClientProperties.getDefaultInterestRatePerPeriod())
        .amortizationType(fineractClientProperties.getDefaultAmortizationType())
        .interestType(fineractClientProperties.getDefaultInterestType())
        .interestCalculationPeriodType(
            fineractClientProperties.getDefaultInterestCalculationPeriodType())
        .transactionProcessingStrategyCode(
            fineractClientProperties.getDefaultTransactionProcessingStrategyCode())
        .expectedDisbursementDate(today)
        .submittedOnDate(today)
        .locale(fineractClientProperties.getLocale())
        .dateFormat(fineractClientProperties.getDateFormat())
        .build();
  }
}
