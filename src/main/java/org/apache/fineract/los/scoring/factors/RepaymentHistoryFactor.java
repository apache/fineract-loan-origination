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
 * Scores the applicant's past repayment behaviour in Fineract.
 *
 * Default weight: 15 points (configurable via {@code los.scoring.weights.repayment-history}).
 *
 * Critical design decision: first-time borrowers with no history (both counts null or zero) are
 * scored at the <strong>midpoint</strong> rather than zero. Penalising first-time borrowers as if
 * they had bad history would unfairly exclude new applicants — common in microfinance where many
 * borrowers have no formal credit history.
 *
 * All arithmetic uses {@link BigDecimal} exclusively — never mixed with {@code float}/{@code
 * double} — to avoid precision drift on financial calculations.
 */
@Component
@RequiredArgsConstructor
public class RepaymentHistoryFactor implements ScoringFactor {

  private static final String FACTOR_NAME = "repayment-history";

  private final ScoringWeightsProperties weights;

  @Override
  public FactorScore score(final ApplicantScoringProfile profile) {
    final int max = maxPoints();

    final int successful = nullToZero(profile.getSuccessfulRepaymentsCount());
    final int missed = nullToZero(profile.getMissedRepaymentsCount());
    final int total = successful + missed;

    if (total == 0) {
      final int midpoint =
          BigDecimal.valueOf(max).divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP).intValue();
      return buildScore(
          midpoint,
          max,
          "No prior repayment history — first-time "
              + "borrower scored at midpoint rather "
              + "than penalised.");
    }

    final BigDecimal successRate =
        BigDecimal.valueOf(successful).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);

    final int points =
        BigDecimal.valueOf(max).multiply(successRate).setScale(0, RoundingMode.HALF_UP).intValue();

    final BigDecimal successPercent =
        successRate.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);

    return buildScore(
        points,
        max,
        String.format(
            "Repayment history: %d successful, %d missed " + "out of %d total — %s%% success rate.",
            successful, missed, total, successPercent));
  }

  @Override
  public int maxPoints() {
    return weights.getRepaymentHistory();
  }

  @Override
  public String factorName() {
    return FACTOR_NAME;
  }

  private int nullToZero(final Integer value) {
    return value == null ? 0 : Math.max(0, value);
  }

  private FactorScore buildScore(final int points, final int max, final String explanation) {
    return FactorScore.builder().points(points).maxPoints(max).explanation(explanation).build();
  }
}
