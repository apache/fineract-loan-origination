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

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.domain.ApplicantProfile;
import org.apache.fineract.los.domain.CreditScore;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.exception.ApplicantProfileNotFoundException;
import org.apache.fineract.los.repository.ApplicantProfileRepository;
import org.apache.fineract.los.repository.CreditScoreRepository;
import org.apache.fineract.los.scoring.CreditScoringStrategy;
import org.apache.fineract.los.scoring.model.ApplicantScoringProfile;
import org.apache.fineract.los.scoring.model.CreditScoreResult;
import org.apache.fineract.los.scoring.model.FactorScore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges the pure {@link CreditScoringStrategy} scoring engine to persistence.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Builds the immutable {@link ApplicantScoringProfile} input from the {@link
 *       ApplicantProfile} and {@link LoanApplication} entities
 *   <li>Delegates the actual scoring computation to {@link CreditScoringStrategy}
 *   <li>Maps the resulting {@link CreditScoreResult} — including every individual {@link
 *       FactorScore} — onto a persisted {@link CreditScore} entity
 * </ul>
 *
 * <p>{@link CreditScore} is documented as immutable after creation and computed once when an
 * application enters UNDER_REVIEW. This service honours that contract: if a score already exists
 * for the application, {@link #computeAndPersist} returns the existing record unchanged rather than
 * recomputing — recomputation would silently invalidate an auditable, explainability-bearing
 * record.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditScoringService {

  private final CreditScoringStrategy creditScoringStrategy;
  private final CreditScoreRepository creditScoreRepository;
  private final ApplicantProfileRepository applicantProfileRepository;

  /**
   * Computes and persists a credit score for the given application, unless one already exists.
   *
   * @param application the loan application to score — must already have a linked {@code
   *     ApplicantProfile}
   * @return the persisted (or pre-existing) {@link CreditScore} entity
   */
  @Transactional
  public CreditScore computeAndPersist(final LoanApplication application) {
    if (application == null) {
      throw new IllegalArgumentException("LoanApplication must not be null");
    }

    return creditScoreRepository
        .findByApplication(application)
        .map(
            existing -> {
              log.info(
                  "Credit score already exists for applicationRef={} — skipping "
                      + "recomputation (score is immutable after creation).",
                  application.getApplicationRef());
              return existing;
            })
        .orElseGet(() -> computeAndSave(application));
  }

  /**
   * Returns the persisted credit score for an application, if one has been computed.
   *
   * @param application the loan application
   * @return the existing score, or empty if scoring hasn't run yet (application not yet
   *     UNDER_REVIEW)
   */
  public java.util.Optional<org.apache.fineract.los.domain.CreditScore> findExistingScore(
      final LoanApplication application) {
    return creditScoreRepository.findByApplication(application);
  }

  private CreditScore computeAndSave(final LoanApplication application) {
    final ApplicantProfile profile =
        applicantProfileRepository
            .findByApplication(application)
            .orElseThrow(
                () ->
                    new ApplicantProfileNotFoundException(
                        application.getApplicationRef(), application.getTenantId()));

    final ApplicantScoringProfile scoringProfile = toScoringProfile(application, profile);
    final CreditScoreResult result = creditScoringStrategy.calculate(scoringProfile);

    final CreditScore entity = new CreditScore();
    entity.setApplication(application);
    entity.setTenantId(application.getTenantId());
    entity.setScore(result.getScore());
    entity.setRiskCategory(result.getRiskCategory());
    entity.setScoredAt(LocalDateTime.now());
    entity.setIncomeRatioScore(pointsOrZero(result, "income-ratio"));
    entity.setDebtBurdenScore(pointsOrZero(result, "debt-burden"));
    entity.setEmploymentScore(pointsOrZero(result, "employment-stability"));
    entity.setRepaymentHistoryScore(pointsOrZero(result, "repayment-history"));
    entity.setLoanPurposeScore(pointsOrZero(result, "loan-purpose-risk"));

    final CreditScore saved = creditScoreRepository.save(entity);

    log.info(
        "Credit score persisted: applicationRef={} score={} riskCategory={}",
        application.getApplicationRef(),
        saved.getScore(),
        saved.getRiskCategory());

    return saved;
  }

  /**
   * Builds the scoring engine's input model from the applicant profile and application.
   *
   * <p>{@code successfulRepaymentsCount} and {@code missedRepaymentsCount} are left null here —
   * they require querying the applicant's prior loan history from Fineract, which is a natural
   * extension point for {@code FineractLoanApiClient} once a "client loan history" read endpoint is
   * wired in. {@code RepaymentHistoryFactor} explicitly handles null/zero as "first-time borrower,
   * scored at midpoint" rather than penalising — so this is safe today, not a placeholder bug.
   */
  private ApplicantScoringProfile toScoringProfile(
      final LoanApplication application, final ApplicantProfile profile) {
    return ApplicantScoringProfile.builder()
        .monthlyIncome(profile.getMonthlyIncome())
        .requestedAmount(application.getRequestedAmount())
        .existingLoanObligations(profile.getExistingLoanObligations())
        .employmentStatus(profile.getEmploymentStatus())
        .employmentDurationMonths(profile.getEmploymentDurationMonths())
        .successfulRepaymentsCount(null)
        .missedRepaymentsCount(null)
        .loanPurpose(application.getLoanPurpose())
        .build();
  }

  private int pointsOrZero(final CreditScoreResult result, final String factorName) {
    final FactorScore factorScore = result.getFactorScores().get(factorName);
    return factorScore == null ? 0 : factorScore.getPoints();
  }
}
