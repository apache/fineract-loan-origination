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

import org.apache.fineract.los.domain.enums.LoanPurpose;
import org.apache.fineract.los.scoring.ScoringWeightsProperties;
import org.apache.fineract.los.scoring.model.ApplicantScoringProfile;
import org.apache.fineract.los.scoring.model.FactorScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/** Unit tests for {@link LoanPurposeRiskFactor}. */
@DisplayName("LoanPurposeRiskFactor")
class LoanPurposeRiskFactorTest {

  private LoanPurposeRiskFactor factor;

  @BeforeEach
  void setUp() {
    factor = new LoanPurposeRiskFactor(new ScoringWeightsProperties());
  }

  @Test
  @DisplayName("maxPoints returns configured loan-purpose-risk weight")
  void maxPointsReturnsConfiguredWeight() {
    assertThat(factor.maxPoints()).isEqualTo(10);
  }

  @Test
  @DisplayName("factorName returns stable identifier")
  void factorNameIsStable() {
    assertThat(factor.factorName()).isEqualTo("loan-purpose-risk");
  }

  @ParameterizedTest(name = "purpose={0} expectedPoints={1}")
  @DisplayName("Each purpose category produces expected points")
  @CsvSource({
    "AGRICULTURE,     10",
    "EDUCATION,       10",
    "BUSINESS,        8",
    "HOME_IMPROVEMENT, 8",
    "CONSUMER,        5",
    "MEDICAL,         5",
    "SPECULATION,     3",
    "OTHER,           3",
  })
  void purposeCategories(final String purpose, final int expectedPoints) {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder().loanPurpose(purpose).build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(expectedPoints);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Null or blank purpose scores neutral default")
  void nullOrBlankPurposeScoresNeutral(final String purpose) {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder().loanPurpose(purpose).build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(5); // 50% of 10
    assertThat(result.getExplanation()).contains("UNSPECIFIED");
  }

  @Test
  @DisplayName("Unrecognised purpose scores neutral default")
  void unrecognisedPurposeScoresNeutral() {
    // Intentionally not a LoanPurpose enum constant — this
    // test asserts behaviour for values outside the known set.
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder().loanPurpose("VACATION").build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(5);
  }

  @Test
  @DisplayName("Purpose comparison is case-insensitive")
  void purposeComparisonIsCaseInsensitive() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .loanPurpose(LoanPurpose.AGRICULTURE.name().toLowerCase())
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(10);
  }

  @Test
  @DisplayName("Points never exceed configured maxPoints")
  void pointsNeverExceedMaxPoints() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder().loanPurpose(LoanPurpose.EDUCATION.name()).build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isLessThanOrEqualTo(factor.maxPoints());
  }
}
