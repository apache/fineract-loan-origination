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

import org.apache.fineract.los.domain.enums.EmploymentStatus;
import org.apache.fineract.los.scoring.ScoringWeightsProperties;
import org.apache.fineract.los.scoring.model.ApplicantScoringProfile;
import org.apache.fineract.los.scoring.model.FactorScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link EmploymentStabilityFactor}. */
@DisplayName("EmploymentStabilityFactor")
class EmploymentStabilityFactorTest {

  private EmploymentStabilityFactor factor;

  @BeforeEach
  void setUp() {
    factor = new EmploymentStabilityFactor(new ScoringWeightsProperties());
  }

  @Test
  @DisplayName("maxPoints returns configured employment-stability weight")
  void maxPointsReturnsConfiguredWeight() {
    assertThat(factor.maxPoints()).isEqualTo(20);
  }

  @Test
  @DisplayName("factorName returns stable identifier")
  void factorNameIsStable() {
    assertThat(factor.factorName()).isEqualTo("employment-stability");
  }

  @Test
  @DisplayName("EMPLOYED with long tenure scores maximum points")
  void employedLongTenureScoresMaximum() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .employmentStatus(EmploymentStatus.EMPLOYED.name())
            .employmentDurationMonths(48)
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(20);
  }

  @Test
  @DisplayName("UNEMPLOYED scores zero for status portion but duration still counts")
  void unemployedStatusPortionScoresZero() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .employmentStatus(EmploymentStatus.UNEMPLOYED.name())
            .employmentDurationMonths(60)
            .build();

    final FactorScore result = factor.score(profile);

    // Status contributes 0 (UNEMPLOYED), duration still
    // contributes 40% = 8 points regardless of status —
    // this is the factor's intended design, not a bug.
    assertThat(result.getPoints()).isEqualTo(8);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Null or blank employment status scores zero for status portion")
  void nullOrBlankStatusScoresZeroStatusPortion(final String status) {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .employmentStatus(status)
            .employmentDurationMonths(48)
            .build();

    final FactorScore result = factor.score(profile);

    // Status contributes 0, duration still contributes 40%
    assertThat(result.getPoints()).isEqualTo(8);
  }

  @ParameterizedTest
  @ValueSource(ints = {-5, 0})
  @DisplayName("Null, zero, or negative duration scores zero for duration portion")
  void nonPositiveDurationScoresZeroDurationPortion(final int months) {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .employmentStatus(EmploymentStatus.EMPLOYED.name())
            .employmentDurationMonths(months)
            .build();

    final FactorScore result = factor.score(profile);

    // Status contributes 60%, duration contributes 0
    assertThat(result.getPoints()).isEqualTo(12);
  }

  @Test
  @DisplayName("Null duration scores zero for duration portion")
  void nullDurationScoresZeroDurationPortion() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .employmentStatus(EmploymentStatus.EMPLOYED.name())
            .employmentDurationMonths(null)
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(12);
  }

  @ParameterizedTest(name = "status={0} expectedPoints={1}")
  @DisplayName("Each employment status tier produces expected points")
  @CsvSource({"EMPLOYED,20", "SELF_EMPLOYED,19", "INFORMAL,14", "UNKNOWN_VALUE,14"})
  void statusTiers(final String status, final int expectedPoints) {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .employmentStatus(status)
            .employmentDurationMonths(48)
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(expectedPoints);
  }

  @Test
  @DisplayName("Status comparison is case-insensitive")
  void statusComparisonIsCaseInsensitive() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .employmentStatus(EmploymentStatus.EMPLOYED.name().toLowerCase())
            .employmentDurationMonths(48)
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isEqualTo(20);
  }

  @Test
  @DisplayName("Points never exceed configured maxPoints")
  void pointsNeverExceedMaxPoints() {
    final ApplicantScoringProfile profile =
        ApplicantScoringProfile.builder()
            .employmentStatus(EmploymentStatus.EMPLOYED.name())
            .employmentDurationMonths(1000)
            .build();

    final FactorScore result = factor.score(profile);

    assertThat(result.getPoints()).isLessThanOrEqualTo(factor.maxPoints());
  }
}
