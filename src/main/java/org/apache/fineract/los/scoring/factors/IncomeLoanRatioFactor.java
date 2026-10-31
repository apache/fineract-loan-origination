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
 * Scores the applicant's income-to-loan ratio — the ratio of monthly income to the requested loan
 * amount.
 *
 * Default weight: 30 points (configurable via {@code los.scoring.weights.income-ratio}).
 * Thresholds are also configurable via {@code los.scoring.weights.income-ratio-*} so institutions
 * can tune their own risk appetite without code changes.
 *
 * All arithmetic uses {@link BigDecimal} exclusively — never mixed with {@code float}/{@code
 * double} — to avoid precision drift on financial calculations.
 *
 * Division by zero is explicitly guarded: a zero or negative requested amount is treated as
 * invalid input and scored as zero rather than throwing {@link ArithmeticException}.
 */
@Component
@RequiredArgsConstructor
public class IncomeLoanRatioFactor implements ScoringFactor {

  private static final String FACTOR_NAME = "income-ratio";
  private static final int SCALE = 4;

  private final ScoringWeightsProperties weights;

  @Override
  public FactorScore score(final ApplicantScoringProfile profile) {
    final int max = maxPoints();

    if (!hasValidInputs(profile)) {
      return buildScore(
          0,
          max,
          "Income or requested amount missing or invalid "
              + "— factor scored as zero pending "
              + "complete data.");
    }

    final BigDecimal ratio =
        profile
            .getMonthlyIncome()
            .divide(profile.getRequestedAmount(), SCALE, RoundingMode.HALF_UP);

    final int points;
    final String tier;

    if (ratio.compareTo(weights.getIncomeRatioFullThreshold()) >= 0) {
      points = max;
      tier = "strong";
    } else if (ratio.compareTo(weights.getIncomeRatioHighThreshold()) >= 0) {
      points = scaled(max, 75);
      tier = "good";
    } else if (ratio.compareTo(weights.getIncomeRatioMediumThreshold()) >= 0) {
      points = scaled(max, 50);
      tier = "moderate";
    } else if (ratio.compareTo(BigDecimal.ZERO) > 0) {
      points = scaled(max, 25);
      tier = "weak";
    } else {
      points = 0;
      tier = "none";
    }

    return buildScore(
        points,
        max,
        String.format(
            "Income-to-loan ratio of %s indicates %s " + "repayment capacity.", ratio, tier));
  }

  @Override
  public int maxPoints() {
    return weights.getIncomeRatio();
  }

  @Override
  public String factorName() {
    return FACTOR_NAME;
  }

  /**
   * Validates that both income and requested amount are present, and that requested amount is
   * strictly positive — preventing division by zero in {@link #score}.
   */
  private boolean hasValidInputs(final ApplicantScoringProfile profile) {
    return profile.getMonthlyIncome() != null
        && profile.getRequestedAmount() != null
        && profile.getRequestedAmount().compareTo(BigDecimal.ZERO) > 0;
  }

  /**
   * Scales a maximum point value by an integer percentage using exact {@link BigDecimal} arithmetic
   * throughout — no {@code float}/{@code double} intermediate values.
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
