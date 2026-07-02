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
 * Unit tests for {@link IncomeLoanRatioFactor}.
 *
 * <p>Specifically covers the review feedback that previously caused {@link ArithmeticException} on
 * a zero requested amount — verifies the guard now prevents this entirely.
 */
@DisplayName("IncomeLoanRatioFactor")
class IncomeLoanRatioFactorTest {

  private IncomeLoanRatioFactor factor;
  private ScoringWeightsProperties weights;

  @BeforeEach
  void setUp() {
    weights = new ScoringWeightsProperties();
    factor = new IncomeLoanRatioFactor(weights);
  }

  @Test
  @DisplayName("maxPoints returns configured income-ratio weight")
  void maxPointsReturnsConfiguredWeight() {
    assertThat(factor.maxPoints()).isEqualTo(30);
  }

  @Test
  @DisplayName("factorName returns stable identifier")
  void factorNameIsStable() {
    assertThat(factor.factorName()).isEqualTo("income-ratio");
  }

  @ParameterizedTest(name = "income={0} requested={1} expectedPoints={2}")
  @DisplayName("Ratio tiers produce expected points")
  @CsvSource({
    // income, requested, expectedPoints (max=30)
    "10000, 50000, 30", // ratio 0.20 -> full
    "6000,  50000, 23", // ratio 0.12 -> 75% = 22.5 -> 23 (HALF_UP)
    "3000,  50000, 15", // ratio 0.06 -> 50%
    "1000,  50000, 8", // ratio 0.02 -> 25% = 7.5 -> 8 (HALF_UP)
    "0,     50000, 0", // ratio 0.00 -> none
  })
  void ratioTiers(final String income, final String requested, final int expectedPoints) {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .monthlyIncome(new BigDecimal(income))
            .requestedAmount(new BigDecimal(requested))
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(expectedPoints);
    assertThat(result.getMaxPoints()).isEqualTo(30);
    assertThat(result.getExplanation()).isNotBlank();
  }

  @Test
  @DisplayName("Zero requested amount does not throw and scores zero")
  void zeroRequestedAmountDoesNotThrow() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .monthlyIncome(BigDecimal.valueOf(5000))
            .requestedAmount(BigDecimal.ZERO)
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isZero();
    assertThat(result.getExplanation()).containsIgnoringCase("invalid");
  }

  @Test
  @DisplayName("Negative requested amount does not throw and scores zero")
  void negativeRequestedAmountDoesNotThrow() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .monthlyIncome(BigDecimal.valueOf(5000))
            .requestedAmount(BigDecimal.valueOf(-1000))
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isZero();
  }

  @Test
  @DisplayName("Null monthly income scores zero without throwing")
  void nullMonthlyIncomeScoresZero() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .monthlyIncome(null)
            .requestedAmount(BigDecimal.valueOf(50000))
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isZero();
  }

  @Test
  @DisplayName("Null requested amount scores zero without throwing")
  void nullRequestedAmountScoresZero() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .monthlyIncome(BigDecimal.valueOf(5000))
            .requestedAmount(null)
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isZero();
  }

  @Test
  @DisplayName("Very small non-zero income fraction does not throw")
  void verySmallIncomeFractionDoesNotThrow() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .monthlyIncome(new BigDecimal("0.00001"))
            .requestedAmount(BigDecimal.valueOf(50000))
            .build();

    final FactorScore result = factor.score(profile);

    // Ratio is positive but negligible -> weakest non-zero tier
    assertThat(result.getPoints()).isBetween(0, 30);
  }

  @Test
  @DisplayName("Custom configured thresholds are respected")
  void customThresholdsAreRespected() {
    weights.setIncomeRatioFullThreshold(new BigDecimal("0.50"));
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .monthlyIncome(BigDecimal.valueOf(10000))
            .requestedAmount(BigDecimal.valueOf(50000))
            .build();
    // ratio = 0.20, which previously scored "full" but
    // now does not reach the raised custom threshold of 0.50

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isLessThan(factor.maxPoints());
  }

  @Test
  @DisplayName("Points never exceed configured maxPoints")
  void pointsNeverExceedMaxPoints() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .monthlyIncome(BigDecimal.valueOf(1_000_000))
            .requestedAmount(BigDecimal.valueOf(1000))
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isLessThanOrEqualTo(factor.maxPoints());
  }
}
