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

import lombok.RequiredArgsConstructor;
import org.apache.fineract.los.scoring.ScoringFactor;
import org.apache.fineract.los.scoring.ScoringWeightsProperties;
import org.apache.fineract.los.scoring.model.ApplicantScoringProfile;
import org.apache.fineract.los.scoring.model.FactorScore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Scores the applicant's employment stability based on employment status and duration in current
 * employment.
 *
 * Default weight: 20 points (configurable via {@code los.scoring.weights.employment-stability}).
 *
 * Combines two signals:
 *
 * 
 *   Employment status — EMPLOYED and SELF_EMPLOYED score higher than INFORMAL or UNEMPLOYED
 *   Duration — longer tenure indicates lower job-loss risk
 * 
 *
 * Status contributes 60% of this factor's points, duration contributes the remaining 40% —
 * reflecting that having stable income at all matters more than how long, per CGAP guidance for
 * resource-constrained lending environments.
 */
@Component
@RequiredArgsConstructor
public class EmploymentStabilityFactor implements ScoringFactor {

  private static final String FACTOR_NAME = "employment-stability";

  private static final int STATUS_WEIGHT_PERCENT = 60;
  private static final int DURATION_WEIGHT_PERCENT = 40;

  private static final int DURATION_FULL_MONTHS = 36;
  private static final int DURATION_HIGH_MONTHS = 12;
  private static final int DURATION_MODERATE_MONTHS = 6;

  private final ScoringWeightsProperties weights;

  @Override
  public FactorScore score(final ApplicantScoringProfile profile) {
    final int max = maxPoints();

    final int statusPoints = scoreStatus(profile.getEmploymentStatus(), max);
    final int durationPoints = scoreDuration(profile.getEmploymentDurationMonths(), max);

    final int totalPoints = Math.min(max, statusPoints + durationPoints);

    return FactorScore.builder()
        .points(totalPoints)
        .maxPoints(max)
        .explanation(
            String.format(
                "Employment status [%s] with %s tenure " + "contributes %d of %d points.",
                StringUtils.hasText(profile.getEmploymentStatus())
                    ? profile.getEmploymentStatus()
                    : "UNKNOWN",
                durationDescription(profile.getEmploymentDurationMonths()),
                totalPoints,
                max))
        .build();
  }

  @Override
  public int maxPoints() {
    return weights.getEmploymentStability();
  }

  @Override
  public String factorName() {
    return FACTOR_NAME;
  }

  private int scoreStatus(final String status, final int max) {
    final int statusMax = scaled(max, STATUS_WEIGHT_PERCENT);

    if (!StringUtils.hasText(status)) {
      return 0;
    }

    return switch (status.toUpperCase()) {
      case "EMPLOYED" -> statusMax;
      case "SELF_EMPLOYED" -> scaled(statusMax, 90);
      case "INFORMAL" -> scaled(statusMax, 50);
      case "UNEMPLOYED" -> 0;
      default -> scaled(statusMax, 50);
    };
  }

  private int scoreDuration(final Integer durationMonths, final int max) {
    final int durationMax = scaled(max, DURATION_WEIGHT_PERCENT);

    if (durationMonths == null || durationMonths <= 0) {
      return 0;
    }
    if (durationMonths >= DURATION_FULL_MONTHS) {
      return durationMax;
    }
    if (durationMonths >= DURATION_HIGH_MONTHS) {
      return scaled(durationMax, 75);
    }
    if (durationMonths >= DURATION_MODERATE_MONTHS) {
      return scaled(durationMax, 50);
    }
    return scaled(durationMax, 25);
  }

  private String durationDescription(final Integer months) {
    if (months == null || months <= 0) {
      return "unknown";
    }
    if (months >= DURATION_FULL_MONTHS) {
      return "long-term (3+ years)";
    }
    if (months >= DURATION_HIGH_MONTHS) {
      return "established (1-3 years)";
    }
    return "recent (under 1 year)";
  }

  private int scaled(final int max, final int percentage) {
    return Math.round(max * percentage / 100.0f);
  }
}
