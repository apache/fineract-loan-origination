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

import org.apache.fineract.los.scoring.ScoringWeightsProperties;
import org.apache.fineract.los.scoring.model.ApplicantScoringProfile;
import org.apache.fineract.los.scoring.model.FactorScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link RepaymentHistoryFactor}.
 *
 * <p>Verifies the BigDecimal-based rewrite produces identical results to the original float-based
 * logic for representative cases, with no precision drift.
 */
@DisplayName("RepaymentHistoryFactor")
class RepaymentHistoryFactorTest {

  private RepaymentHistoryFactor factor;

  @BeforeEach
  void setUp() {
    factor = new RepaymentHistoryFactor(new ScoringWeightsProperties());
  }

  @Test
  @DisplayName("maxPoints returns configured repayment-history weight")
  void maxPointsReturnsConfiguredWeight() {
    assertThat(factor.maxPoints()).isEqualTo(15);
  }

  @Test
  @DisplayName("factorName returns stable identifier")
  void factorNameIsStable() {
    assertThat(factor.factorName()).isEqualTo("repayment-history");
  }

  @Test
  @DisplayName("First-time borrower with no history scores midpoint")
  void firstTimeBorrowerScoresMidpoint() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .successfulRepaymentsCount(null)
            .missedRepaymentsCount(null)
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(8); // round(15/2) = 8
    assertThat(result.getExplanation()).containsIgnoringCase("first-time");
  }

  @Test
  @DisplayName("Zero successful and zero missed scores midpoint")
  void zeroBothCountsScoresMidpoint() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .successfulRepaymentsCount(0)
            .missedRepaymentsCount(0)
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(8);
  }

  @ParameterizedTest(name = "successful={0} missed={1} expectedPoints={2}")
  @DisplayName("Repayment ratio tiers produce expected points")
  @CsvSource({
    "10, 0,  15", // 100% success -> full
    "9,  1,  14", // 90% success of 15 = 13.5 -> 14 (HALF_UP)
    "5,  5,  8", // 50% success of 15 = 7.5 -> 8 (HALF_UP)
    "1,  9,  2", // 10% success of 15 = 1.5 -> 2 (HALF_UP)
    "0,  10, 0", // 0% success -> zero
  })
  void repaymentRatioTiers(final int successful, final int missed, final int expectedPoints) {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .successfulRepaymentsCount(successful)
            .missedRepaymentsCount(missed)
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(expectedPoints);
  }

  @Test
  @DisplayName("Negative counts are treated as zero, not subtracted")
  void negativeCountsTreatedAsZero() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .successfulRepaymentsCount(-5)
            .missedRepaymentsCount(-3)
            .build();

    final FactorScore result = factor.score(profile);

    // Both treated as zero -> total is zero -> midpoint
    assertThat(result.getPoints()).isEqualTo(8);
  }

  @Test
  @DisplayName("Explanation includes success rate percentage")
  void explanationIncludesSuccessRate() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .successfulRepaymentsCount(8)
            .missedRepaymentsCount(2)
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getExplanation())
        .contains("8 successful")
        .contains("2 missed")
        .contains("10 total")
        .contains("80%");
  }

  @Test
  @DisplayName("Points never exceed configured maxPoints")
  void pointsNeverExceedMaxPoints() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .successfulRepaymentsCount(1000)
            .missedRepaymentsCount(0)
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isLessThanOrEqualTo(factor.maxPoints());
  }

  @Test
  @DisplayName("Rounding is exact with small maxPoints — no float drift")
  void roundingExactWithSmallMaxPoints() {
    final ScoringWeightsProperties weights = new ScoringWeightsProperties();
    weights.setRepaymentHistory(5);
    final RepaymentHistoryFactor smallMaxFactor = new RepaymentHistoryFactor(weights);

    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .successfulRepaymentsCount(3)
            .missedRepaymentsCount(7)
            .build();
    // 30% success of 5 = 1.5 -> 2 (HALF_UP)

    final FactorScore result = smallMaxFactor.score(profile);

    assertThat(result.getPoints()).isEqualTo(2);
  }
}
