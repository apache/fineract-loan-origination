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

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.los.scoring.ScoringFactor;
import org.apache.fineract.los.scoring.ScoringWeightsProperties;
import org.apache.fineract.los.scoring.model.ApplicantScoringProfile;
import org.apache.fineract.los.scoring.model.FactorScore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Scores risk based on the declared purpose of the loan.
 *
 * Default weight: 10 points (configurable via {@code los.scoring.weights.loan-purpose-risk}).
 *
 * Purpose categories are mapped to a risk multiplier based on income stability and repayment
 * predictability typically associated with each category in microfinance lending:
 *
 * 
 *   AGRICULTURE, EDUCATION — low risk, full points
 *   BUSINESS, HOME_IMPROVEMENT — moderate risk, 75%
 *   CONSUMER, MEDICAL — moderate-high risk, 50%
 *   SPECULATION, OTHER — high risk, 25%
 *   unrecognised or missing purpose — 50% (neutral)
 * 
 *
 * Category mapping is intentionally a simple in-memory map rather than externalised
 * configuration — unlike the numeric weights, these categorical risk tiers are a domain modelling
 * decision rather than an institution-tunable parameter. If a future institution needs different
 * categories, this can be extended to read from configuration without changing the {@link
 * ScoringFactor} contract.
 */
@Component
@RequiredArgsConstructor
public class LoanPurposeRiskFactor implements ScoringFactor {

  private static final String FACTOR_NAME = "loan-purpose-risk";

  private static final int DEFAULT_PERCENTAGE = 50;

  private static final Map<String, Integer> PURPOSE_RISK_PERCENTAGE =
      Map.of(
          "AGRICULTURE", 100,
          "EDUCATION", 100,
          "BUSINESS", 75,
          "HOME_IMPROVEMENT", 75,
          "CONSUMER", 50,
          "MEDICAL", 50,
          "SPECULATION", 25,
          "OTHER", 25);

  private final ScoringWeightsProperties weights;

  @Override
  public FactorScore score(final ApplicantScoringProfile profile) {
    final int max = maxPoints();
    final String purpose = profile.getLoanPurpose();

    final int percentage = resolvePercentage(purpose);
    final int points = Math.round(max * percentage / 100.0f);

    return FactorScore.builder()
        .points(points)
        .maxPoints(max)
        .explanation(
            String.format(
                "Loan purpose [%s] classified as %s risk.",
                StringUtils.hasText(purpose) ? purpose : "UNSPECIFIED", riskLabel(percentage)))
        .build();
  }

  @Override
  public int maxPoints() {
    return weights.getLoanPurposeRisk();
  }

  @Override
  public String factorName() {
    return FACTOR_NAME;
  }

  private int resolvePercentage(final String purpose) {
    if (!StringUtils.hasText(purpose)) {
      return DEFAULT_PERCENTAGE;
    }
    return PURPOSE_RISK_PERCENTAGE.getOrDefault(purpose.toUpperCase(), DEFAULT_PERCENTAGE);
  }

  private String riskLabel(final int percentage) {
    if (percentage >= 100) {
      return "low";
    }
    if (percentage >= 75) {
      return "moderate";
    }
    if (percentage >= 50) {
      return "moderate-high";
    }
    return "high";
  }
}
