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
import org.apache.fineract.los.domain.ApplicantProfile;
import org.apache.fineract.los.domain.LoanApplication;
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
 * <p>Triggered when an application reaches {@code APPROVED}. Builds a {@link
 * FineractLoanCreateRequest} from the origination data collected during the LOS lifecycle, calls
 * {@link FineractLoanApiClient#createLoan}, records the returned Fineract {@code loanId} on the
 * application, and finally drives the state machine from APPROVED to DISBURSED.
 *
 * <p>Per the proposal's architecture: "The bridge service automatically calls Fineract's existing
 * POST /loans API with the parameters collected during origination... falls back to mock responses
 * if FINERACT-2418 is not yet available" — the choice of real vs. mock adapter is entirely
 * delegated to {@link FineractLoanApiClient}; this class is unaware of which implementation is
 * active.
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
   * Disburses an APPROVED application by creating the corresponding loan in Fineract, then
   * transitions the application to DISBURSED.
   *
   * @param applicationRef human-readable application reference
   * @param tenantId institution identifier
   * @return the Fineract response identifying the created loan
   */
  @Transactional
  public FineractLoanCreateResponse disburse(final String applicationRef, final String tenantId) {

    final LoanApplication application =
        loanApplicationRepository
            .findByApplicationRefAndTenantId(applicationRef, tenantId)
            .orElseThrow(() -> new ApplicationNotFoundException(applicationRef, tenantId));

    validateApproved(application);

    final ApplicantProfile profile =
        applicantProfileRepository
            .findByApplication(application)
            .orElseThrow(() -> new ApplicantProfileNotFoundException(applicationRef, tenantId));

    validateFineractLinkage(application, profile);

    final FineractLoanCreateRequest request = buildRequest(application, profile);

    final FineractLoanCreateResponse response;
    try {
      response = fineractLoanApiClient.createLoan(request);
    } catch (final RestClientException ex) {
      log.error(
          "Disbursement bridge call to Fineract failed for applicationRef={}", applicationRef, ex);
      throw new FineractIntegrationException(applicationRef, ex);
    }

    application.setFineractLoanId(response.getLoanId());
    stateMachine.transition(application, LoanApplicationStatus.DISBURSED);
    loanApplicationRepository.save(application);

    log.info(
        "Disbursement bridge completed: applicationRef={} fineractLoanId={}",
        applicationRef,
        response.getLoanId());

    return response;
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
  private FineractLoanCreateRequest buildRequest(
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
        .transactionProcessingStrategyCode(
            fineractClientProperties.getDefaultTransactionProcessingStrategyCode())
        .expectedDisbursementDate(today)
        .submittedOnDate(today)
        .locale(fineractClientProperties.getLocale())
        .dateFormat(fineractClientProperties.getDateFormat())
        .build();
  }
}
