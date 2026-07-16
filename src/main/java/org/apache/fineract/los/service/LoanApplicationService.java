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

package org.apache.fineract.los.service;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.domain.ApplicantProfile;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.dto.request.CreateLoanApplicationRequest;
import org.apache.fineract.los.exception.ApplicantProfileNotFoundException;
import org.apache.fineract.los.exception.ApplicationNotFoundException;
import org.apache.fineract.los.exception.LosErrorConstants;
import org.apache.fineract.los.repository.ApplicantProfileRepository;
import org.apache.fineract.los.repository.LoanApplicationRepository;
import org.apache.fineract.los.statemachine.LoanOriginationStateMachine;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Service layer responsible for the lifecycle of a {@link LoanApplication}: creation, submission,
 * and movement into review.
 *
 * <p>All status mutation is delegated exclusively to {@link LoanOriginationStateMachine} — this
 * class never calls {@code application.setStatus(...)} directly, matching the design contract
 * documented on {@link LoanApplication}.
 *
 * <p>Credit scoring is triggered from here (via {@link CreditScoringService}) the moment an
 * application first enters {@code UNDER_REVIEW} — the state where the entity's own Javadoc says
 * scoring "runs automatically on entry to this state."
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanApplicationService {

  private static final int MAX_REFERENCE_GENERATION_ATTEMPTS = 20;
  private static final String REFERENCE_TEMPLATE = "LOS-%d-%05d";

  private final LoanApplicationRepository loanApplicationRepository;
  private final ApplicantProfileRepository applicantProfileRepository;
  private final LoanOriginationStateMachine stateMachine;
  private final CreditScoringService creditScoringService;

  /**
   * Creates a new loan application together with its linked applicant profile, in DRAFT status.
   *
   * @param request validated creation payload
   * @param tenantId institution identifier from the request header
   * @return the persisted, DRAFT-status application
   */
  @Transactional
  public LoanApplication createApplication(
      final CreateLoanApplicationRequest request, final String tenantId) {

    if (request == null) {
      throw new IllegalArgumentException("CreateLoanApplicationRequest must not be null");
    }
    if (!StringUtils.hasText(tenantId)) {
      throw new IllegalArgumentException("tenantId must not be blank");
    }

    final LoanApplication application = new LoanApplication();
    application.setTenantId(tenantId);
    application.setStatus(LoanApplicationStatus.DRAFT);
    application.setRequestedAmount(request.getRequestedAmount());
    application.setCurrency(
        StringUtils.hasText(request.getCurrency()) ? request.getCurrency() : "USD");
    application.setLoanPurpose(request.getLoanPurpose());
    application.setTenorMonths(request.getTenorMonths());
    application.setFineractLoanProductId(request.getFineractLoanProductId());
    application.setApplicationRef(generateApplicationRef(tenantId));

    final LoanApplication saved = loanApplicationRepository.save(application);

    final ApplicantProfile profile = new ApplicantProfile();
    profile.setApplication(saved);
    profile.setTenantId(tenantId);
    profile.setFullName(request.getApplicant().getFullName());
    profile.setNationalId(request.getApplicant().getNationalId());
    profile.setMonthlyIncome(request.getApplicant().getMonthlyIncome());
    profile.setEmploymentStatus(request.getApplicant().getEmploymentStatus());
    profile.setEmploymentDurationMonths(request.getApplicant().getEmploymentDurationMonths());
    profile.setExistingLoanObligations(
        request.getApplicant().getExistingLoanObligations() == null
            ? BigDecimal.ZERO
            : request.getApplicant().getExistingLoanObligations());
    profile.setFineractClientId(request.getApplicant().getFineractClientId());

    applicantProfileRepository.save(profile);

    log.info(
        "Loan application created: applicationRef={} tenantId={} requestedAmount={}",
        saved.getApplicationRef(),
        tenantId,
        saved.getRequestedAmount());

    return saved;
  }

  /**
   * Submits a DRAFT application — transitions it to SUBMITTED via the state machine.
   *
   * @param applicationRef human-readable application reference
   * @param tenantId institution identifier
   * @return the updated application
   */
  @Transactional
  public LoanApplication submit(final String applicationRef, final String tenantId) {
    final LoanApplication application = getApplicationOrThrow(applicationRef, tenantId);
    stateMachine.transition(application, LoanApplicationStatus.SUBMITTED);
    return loanApplicationRepository.save(application);
  }

  /**
   * Returns all loan applications for the given tenant.
   *
   * <p>Used by the dashboard/list endpoint.
   *
   * @param tenantId institution identifier
   * @return all applications for the tenant
   */
  @Transactional(readOnly = true)
  public List<LoanApplication> getAllApplications(final String tenantId) {

    return loanApplicationRepository.findAllByTenantId(tenantId, Pageable.unpaged()).getContent();
  }

  /**
   * Moves a SUBMITTED or REFERRED application into UNDER_REVIEW via the state machine, then
   * triggers credit scoring for the application (idempotent — a previously computed score is never
   * recomputed, per {@link CreditScoringService}).
   *
   * @param applicationRef human-readable application reference
   * @param tenantId institution identifier
   * @return the updated application
   */
  @Transactional
  public LoanApplication moveToUnderReview(final String applicationRef, final String tenantId) {
    final LoanApplication application = getApplicationOrThrow(applicationRef, tenantId);

    stateMachine.transition(application, LoanApplicationStatus.UNDER_REVIEW);
    final LoanApplication saved = loanApplicationRepository.save(application);

    creditScoringService.computeAndPersist(saved);

    return saved;
  }

  /**
   * Loads an application by reference and tenant, or throws {@link ApplicationNotFoundException}.
   *
   * @param applicationRef human-readable application reference
   * @param tenantId institution identifier
   * @return the matching application
   */
  public LoanApplication getApplicationOrThrow(final String applicationRef, final String tenantId) {
    return loanApplicationRepository
        .findByApplicationRefAndTenantId(applicationRef, tenantId)
        .orElseThrow(() -> new ApplicationNotFoundException(applicationRef, tenantId));
  }

  /**
   * Loads the applicant profile linked to an application, or throws {@link
   * ApplicantProfileNotFoundException}.
   *
   * @param application the parent loan application
   * @return the linked applicant profile
   */
  public ApplicantProfile getProfileOrThrow(final LoanApplication application) {
    return applicantProfileRepository
        .findByApplication(application)
        .orElseThrow(
            () ->
                new ApplicantProfileNotFoundException(
                    application.getApplicationRef(), application.getTenantId()));
  }

  /**
   * Generates a unique, human-readable application reference in the format LOS-{YEAR}-{SEQUENCE},
   * e.g. LOS-2026-00101.
   *
   * <p>Seeds the sequence from the tenant's total application count and probes forward on collision
   * — sufficient uniqueness guarantee for POC-scale concurrency without requiring a dedicated
   * sequence table.
   *
   * @param tenantId institution identifier
   * @return a reference guaranteed unique for this tenant at generation time
   */
  private String generateApplicationRef(final String tenantId) {
    final int year = Year.now().getValue();
    final long seed = loanApplicationRepository.countByTenantId(tenantId) + 1;

    for (int attempt = 0; attempt < MAX_REFERENCE_GENERATION_ATTEMPTS; attempt++) {
      final String candidate = String.format(REFERENCE_TEMPLATE, year, seed + attempt);
      if (!loanApplicationRepository.existsByApplicationRefAndTenantId(candidate, tenantId)) {
        return candidate;
      }
    }

    throw new IllegalStateException(
        String.format(
            LosErrorConstants.MSG_REFERENCE_GENERATION_FAILED_TEMPLATE,
            tenantId,
            MAX_REFERENCE_GENERATION_ATTEMPTS));
  }
}
