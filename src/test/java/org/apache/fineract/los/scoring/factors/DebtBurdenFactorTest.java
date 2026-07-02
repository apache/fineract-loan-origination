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

package org.apache.fineract.los.scoring.factors;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.apache.fineract.los.scoring.ScoringWeightsProperties;
import org.apache.fineract.los.scoring.model.ApplicantScoringProfile;
import org.apache.fineract.los.scoring.model.FactorScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link DebtBurdenFactor}.
 *
 * <p>Specifically covers the review feedback regarding {@link ArithmeticException} risk on
 * zero/negative income, and rounding precision when mixing scaled percentages.
 */
@DisplayName("DebtBurdenFactor")
class DebtBurdenFactorTest {

  private DebtBurdenFactor factor;
  private ScoringWeightsProperties weights;

  @BeforeEach
  void setUp() {
    weights = new ScoringWeightsProperties();
    factor = new DebtBurdenFactor(weights);
  }

  @Test
  @DisplayName("maxPoints returns configured debt-burden weight")
  void maxPointsReturnsConfiguredWeight() {
    assertThat(factor.maxPoints()).isEqualTo(25);
  }

  @Test
  @DisplayName("factorName returns stable identifier")
  void factorNameIsStable() {
    assertThat(factor.factorName()).isEqualTo("debt-burden");
  }

  @ParameterizedTest(name = "obligations={0} income={1} expectedPoints={2}")
  @DisplayName("Debt burden ratio tiers produce expected points")
  @CsvSource({
    // obligations, income, expectedPoints (max=25)
    "500,  10000, 25", // ratio 0.05 -> minimal
    "2500, 10000, 19", // ratio 0.25 -> manageable (75% of 25 = 18.75 -> 19)
    "4500, 10000, 13", // ratio 0.45 -> elevated (50% of 25 = 12.5 -> 13)
    "6500, 10000, 6", // ratio 0.65 -> high (25% of 25 = 6.25 -> 6)
    "9000, 10000, 0", // ratio 0.90 -> overextended
  })
  void debtBurdenTiers(final String obligations, final String income, final int expectedPoints) {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .existingLoanObligations(new BigDecimal(obligations))
            .monthlyIncome(new BigDecimal(income))
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(expectedPoints);
    assertThat(result.getMaxPoints()).isEqualTo(25);
  }

  @Test
  @DisplayName("Zero income does not throw ArithmeticException")
  void zeroIncomeDoesNotThrow() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .existingLoanObligations(BigDecimal.valueOf(500))
            .monthlyIncome(BigDecimal.ZERO)
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isZero();
    assertThat(result.getExplanation()).containsIgnoringCase("non-positive");
  }

  @Test
  @DisplayName("Negative income does not throw ArithmeticException")
  void negativeIncomeDoesNotThrow() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .existingLoanObligations(BigDecimal.valueOf(500))
            .monthlyIncome(BigDecimal.valueOf(-100))
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isZero();
  }

  @Test
  @DisplayName("Null income does not throw and scores zero")
  void nullIncomeScoresZero() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .existingLoanObligations(BigDecimal.valueOf(500))
            .monthlyIncome(null)
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isZero();
  }

  @Test
  @DisplayName("Null existing obligations treated as zero — best case")
  void nullObligationsTreatedAsZero() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .existingLoanObligations(null)
            .monthlyIncome(BigDecimal.valueOf(10000))
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(factor.maxPoints());
  }

  @Test
  @DisplayName("Very small non-zero income fraction does not throw")
  void verySmallIncomeFractionDoesNotThrow() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .existingLoanObligations(BigDecimal.valueOf(100))
            .monthlyIncome(new BigDecimal("0.00001"))
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isBetween(0, factor.maxPoints());
  }

  @Test
  @DisplayName("Rounding remains exact regardless of small maxPoints")
  void roundingExactWithSmallMaxPoints() {
    weights.setDebtBurden(5);
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .existingLoanObligations(BigDecimal.valueOf(2500))
            .monthlyIncome(BigDecimal.valueOf(10000))
            .build();
    // ratio 0.25 -> manageable tier -> 75% of 5 = 3.75 -> 4 (HALF_UP)

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(4);
    assertThat(result.getMaxPoints()).isEqualTo(5);
  }

  @Test
  @DisplayName("Custom configured thresholds are respected")
  void customThresholdsAreRespected() {
    weights.setDebtBurdenLowThreshold(new BigDecimal("0.50"));
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .existingLoanObligations(BigDecimal.valueOf(4000))
            .monthlyIncome(BigDecimal.valueOf(10000))
            .build();
    // ratio = 0.40, now within the raised "low" threshold of 0.50

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(factor.maxPoints());
  }

  @Test
  @DisplayName("Points never exceed configured maxPoints")
  void pointsNeverExceedMaxPoints() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .existingLoanObligations(BigDecimal.ZERO)
            .monthlyIncome(BigDecimal.valueOf(10000))
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isLessThanOrEqualTo(factor.maxPoints());
  }
}
