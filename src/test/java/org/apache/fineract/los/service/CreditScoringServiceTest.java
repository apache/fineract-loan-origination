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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.fineract.los.domain.ApplicantProfile;
import org.apache.fineract.los.domain.CreditScore;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.RiskCategory;
import org.apache.fineract.los.repository.ApplicantProfileRepository;
import org.apache.fineract.los.repository.CreditScoreRepository;
import org.apache.fineract.los.scoring.CreditScoringStrategy;
import org.apache.fineract.los.scoring.model.CreditScoreResult;
import org.apache.fineract.los.scoring.model.FactorScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditScoringService")
class CreditScoringServiceTest {

  @Mock private CreditScoringStrategy creditScoringStrategy;
  @Mock private CreditScoreRepository creditScoreRepository;
  @Mock private ApplicantProfileRepository applicantProfileRepository;

  private CreditScoringService service;

  @BeforeEach
  void setUp() {
    service =
        new CreditScoringService(
            creditScoringStrategy, creditScoreRepository, applicantProfileRepository);
  }

  private LoanApplication application() {
    final LoanApplication app = new LoanApplication();
    app.setApplicationRef("LOS-2026-00001");
    app.setTenantId("default");
    app.setRequestedAmount(new BigDecimal("10000"));
    app.setLoanPurpose("BUSINESS");
    return app;
  }

  @Test
  @DisplayName("computes and persists a score when none exists yet")
  void computesAndPersistsWhenAbsent() {
    final LoanApplication app = application();
    final ApplicantProfile profile = new ApplicantProfile();
    profile.setMonthlyIncome(new BigDecimal("2000"));
    profile.setExistingLoanObligations(BigDecimal.ZERO);
    profile.setEmploymentStatus("EMPLOYED");
    profile.setEmploymentDurationMonths(24);

    when(creditScoreRepository.findByApplication(app)).thenReturn(Optional.empty());
    when(applicantProfileRepository.findByApplication(app)).thenReturn(Optional.of(profile));

    final Map<String, FactorScore> factorScores = new LinkedHashMap<>();
    factorScores.put(
        "income-ratio", FactorScore.builder().points(30).maxPoints(30).explanation("x").build());
    factorScores.put(
        "debt-burden", FactorScore.builder().points(25).maxPoints(25).explanation("x").build());
    factorScores.put(
        "employment-stability",
        FactorScore.builder().points(20).maxPoints(20).explanation("x").build());
    factorScores.put(
        "repayment-history",
        FactorScore.builder().points(8).maxPoints(15).explanation("x").build());
    factorScores.put(
        "loan-purpose-risk",
        FactorScore.builder().points(8).maxPoints(10).explanation("x").build());

    final CreditScoreResult result =
        CreditScoreResult.builder()
            .score(91)
            .riskCategory(RiskCategory.LOW)
            .factorScores(factorScores)
            .build();

    when(creditScoringStrategy.calculate(any())).thenReturn(result);
    when(creditScoreRepository.save(any(CreditScore.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    final CreditScore saved = service.computeAndPersist(app);

    assertThat(saved.getScore()).isEqualTo(91);
    assertThat(saved.getRiskCategory()).isEqualTo(RiskCategory.LOW);
    assertThat(saved.getIncomeRatioScore()).isEqualTo(30);
    assertThat(saved.getDebtBurdenScore()).isEqualTo(25);
    verify(creditScoreRepository).save(any(CreditScore.class));
  }

  @Test
  @DisplayName("does not recompute when a score already exists")
  void skipsRecomputationWhenPresent() {
    final LoanApplication app = application();
    final CreditScore existing = new CreditScore();
    existing.setScore(70);

    when(creditScoreRepository.findByApplication(app)).thenReturn(Optional.of(existing));

    final CreditScore result = service.computeAndPersist(app);

    assertThat(result).isSameAs(existing);
    verify(creditScoringStrategy, never()).calculate(any());
    verify(creditScoreRepository, never()).save(any());
  }

  @Test
  @DisplayName("throws when applicant profile is missing")
  void throwsWhenProfileMissing() {
    final LoanApplication app = application();
    when(creditScoreRepository.findByApplication(app)).thenReturn(Optional.empty());
    when(applicantProfileRepository.findByApplication(app)).thenReturn(Optional.empty());

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.computeAndPersist(app))
        .isInstanceOf(org.apache.fineract.los.exception.ApplicantProfileNotFoundException.class);
  }
}
