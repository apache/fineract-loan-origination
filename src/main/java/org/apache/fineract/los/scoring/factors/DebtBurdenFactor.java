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

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.los.scoring.ScoringFactor;
import org.apache.fineract.los.scoring.ScoringWeightsProperties;
import org.apache.fineract.los.scoring.model.ApplicantScoringProfile;
import org.apache.fineract.los.scoring.model.FactorScore;
import org.springframework.stereotype.Component;

/**
 * Scores the applicant's existing debt burden — the ratio of existing loan obligations to monthly
 * income.
 *
 * Default weight: 25 points (configurable via {@code los.scoring.weights.debt-burden}).
 * Thresholds are configurable via {@code los.scoring.weights.debt-burden-*}.
 *
 * This factor is inverse to the others — a higher debt burden ratio means lower points.
 *
 * All arithmetic uses {@link BigDecimal} exclusively. Income of zero or negative is explicitly
 * guarded to avoid {@link ArithmeticException} on division — scored as zero with an explanation
 * rather than crashing the request.
 */
@Component
@RequiredArgsConstructor
public class DebtBurdenFactor implements ScoringFactor {

  private static final String FACTOR_NAME = "debt-burden";
  private static final int SCALE = 4;

  private final ScoringWeightsProperties weights;

  @Override
  public FactorScore score(final ApplicantScoringProfile profile) {
    final int max = maxPoints();

    if (!hasValidIncome(profile)) {
      return buildScore(
          0,
          max,
          "Monthly income missing or non-positive — debt "
              + "burden cannot be assessed, scored as "
              + "zero pending complete data.");
    }

    final BigDecimal obligations =
        profile.getExistingLoanObligations() == null
            ? BigDecimal.ZERO
            : profile.getExistingLoanObligations();

    final BigDecimal ratio =
        obligations.divide(profile.getMonthlyIncome(), SCALE, RoundingMode.HALF_UP);

    final int points;
    final String tier;

    if (ratio.compareTo(weights.getDebtBurdenLowThreshold()) <= 0) {
      points = max;
      tier = "minimal";
    } else if (ratio.compareTo(weights.getDebtBurdenModerateThreshold()) <= 0) {
      points = scaled(max, 75);
      tier = "manageable";
    } else if (ratio.compareTo(weights.getDebtBurdenHighThreshold()) <= 0) {
      points = scaled(max, 50);
      tier = "elevated";
    } else if (ratio.compareTo(weights.getDebtBurdenSevereThreshold()) <= 0) {
      points = scaled(max, 25);
      tier = "high";
    } else {
      points = 0;
      tier = "overextended";
    }

    return buildScore(
        points, max, String.format("Existing debt burden ratio of %s is %s.", ratio, tier));
  }

  @Override
  public int maxPoints() {
    return weights.getDebtBurden();
  }

  @Override
  public String factorName() {
    return FACTOR_NAME;
  }

  /**
   * Guards against division by zero — income must be present and strictly positive before the ratio
   * is computed.
   */
  private boolean hasValidIncome(final ApplicantScoringProfile profile) {
    return profile.getMonthlyIncome() != null
        && profile.getMonthlyIncome().compareTo(BigDecimal.ZERO) > 0;
  }

  /**
   * Scales a maximum point value by an integer percentage using exact {@link BigDecimal} arithmetic
   * only — avoids the {@code float}/{@code double} rounding drift flagged in review.
   */
  private int scaled(final int max, final int percentage) {
    return BigDecimal.valueOf(max)
        .multiply(BigDecimal.valueOf(percentage))
        .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP)
        .intValue();
  }

  private FactorScore buildScore(final int points, final int max, final String explanation) {
    return FactorScore.builder().points(points).maxPoints(max).explanation(explanation).build();
  }
}
