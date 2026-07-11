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

package org.apache.fineract.los.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.apache.fineract.los.domain.enums.RiskCategory;
import org.apache.fineract.los.scoring.factors.DebtBurdenFactor;
import org.apache.fineract.los.scoring.factors.EmploymentStabilityFactor;
import org.apache.fineract.los.scoring.factors.IncomeLoanRatioFactor;
import org.apache.fineract.los.scoring.factors.LoanPurposeRiskFactor;
import org.apache.fineract.los.scoring.factors.RepaymentHistoryFactor;
import org.apache.fineract.los.scoring.model.ApplicantScoringProfile;
import org.apache.fineract.los.scoring.model.CreditScoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultCreditScoringStrategy}.
 *
 * <p>Wires the real {@link ScoringWeightsProperties} defaults and all five real {@link
 * ScoringFactor} implementations — this is an integration-of-pure-functions test, not a mock-based
 * one, since the whole point of this class is aggregation across the real factors.
 */
@DisplayName("DefaultCreditScoringStrategy")
class DefaultCreditScoringStrategyTest {

  private DefaultCreditScoringStrategy strategy;

  @BeforeEach
  void setUp() {
    final ScoringWeightsProperties weights = new ScoringWeightsProperties();
    strategy =
        new DefaultCreditScoringStrategy(
            java.util.List.of(
                new IncomeLoanRatioFactor(weights),
                new DebtBurdenFactor(weights),
                new EmploymentStabilityFactor(weights),
                new RepaymentHistoryFactor(weights),
                new LoanPurposeRiskFactor(weights)));
  }

  @Test
  @DisplayName("strong applicant scores LOW risk in 70-100 range")
  void strongApplicantScoresLow() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .monthlyIncome(new BigDecimal("5000"))
            .requestedAmount(new BigDecimal("10000"))
            .existingLoanObligations(BigDecimal.ZERO)
            .employmentStatus("EMPLOYED")
            .employmentDurationMonths(48)
            .successfulRepaymentsCount(10)
            .missedRepaymentsCount(0)
            .loanPurpose("AGRICULTURE")
            .build();

    final CreditScoreResult result = strategy.calculate(profile);

    assertThat(result.getScore()).isBetween(70, 100);
    assertThat(result.getRiskCategory()).isEqualTo(RiskCategory.LOW);
    assertThat(result.getFactorScores()).hasSize(5);
  }

  @Test
  @DisplayName("weak applicant scores HIGH risk in 0-39 range")
  void weakApplicantScoresHigh() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .monthlyIncome(new BigDecimal("500"))
            .requestedAmount(new BigDecimal("50000"))
            .existingLoanObligations(new BigDecimal("450"))
            .employmentStatus("UNEMPLOYED")
            .employmentDurationMonths(0)
            .successfulRepaymentsCount(1)
            .missedRepaymentsCount(9)
            .loanPurpose("SPECULATION")
            .build();

    final CreditScoreResult result = strategy.calculate(profile);

    assertThat(result.getScore()).isBetween(0, 39);
    assertThat(result.getRiskCategory()).isEqualTo(RiskCategory.HIGH);
  }

  @Test
  @DisplayName("first-time borrower is not penalised on repayment history")
  void firstTimeBorrowerScoredAtMidpoint() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .monthlyIncome(new BigDecimal("3000"))
            .requestedAmount(new BigDecimal("10000"))
            .existingLoanObligations(BigDecimal.ZERO)
            .employmentStatus("SELF_EMPLOYED")
            .employmentDurationMonths(24)
            .successfulRepaymentsCount(null)
            .missedRepaymentsCount(null)
            .loanPurpose("BUSINESS")
            .build();

    final CreditScoreResult result = strategy.calculate(profile);

    assertThat(result.getFactorScores().get("repayment-history").getPoints())
        .isEqualTo(8); // midpoint of weight 15, rounded
  }

  @Test
  @DisplayName("null profile throws IllegalArgumentException")
  void nullProfileThrows() {
    assertThatThrownBy(() -> strategy.calculate(null)).isInstanceOf(IllegalArgumentException.class);
  }
}
