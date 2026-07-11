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

package org.apache.fineract.los.scoring;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.domain.enums.RiskCategory;
import org.apache.fineract.los.scoring.model.ApplicantScoringProfile;
import org.apache.fineract.los.scoring.model.CreditScoreResult;
import org.apache.fineract.los.scoring.model.FactorScore;
import org.springframework.stereotype.Component;

/**
 * Default rule-based implementation of {@link CreditScoringStrategy}.
 *
 * <p>Discovers every {@link ScoringFactor} bean via constructor injection — Spring collects all
 * beans implementing the interface into the {@code List<ScoringFactor>} automatically. Adding a new
 * factor to the engine requires only implementing {@link ScoringFactor} and annotating it
 * {@code @Component} — this class never changes.
 *
 * <p>The composite score is the sum of every factor's points. Because each {@link ScoringFactor} is
 * individually bounded by its configured weight ({@link ScoringFactor#maxPoints()}), and {@link
 * ScoringWeightsProperties} validates on startup that all weights sum to 100, the composite score
 * is guaranteed to fall within 0-100 without any additional clamping here.
 *
 * <p>{@link #calculate} is a pure function — no persistence, no Fineract calls. Mapping the result
 * onto the {@code CreditScore} entity is the responsibility of {@code CreditScoringService}.
 */
@Slf4j
@Component
public class DefaultCreditScoringStrategy implements CreditScoringStrategy {

  private final List<ScoringFactor> factors;

  public DefaultCreditScoringStrategy(final List<ScoringFactor> factors) {
    this.factors = List.copyOf(factors);
  }

  @Override
  public CreditScoreResult calculate(final ApplicantScoringProfile profile) {
    if (profile == null) {
      throw new IllegalArgumentException("ApplicantScoringProfile must not be null");
    }

    final Map<String, FactorScore> factorScores = new LinkedHashMap<>();
    int total = 0;

    for (final ScoringFactor factor : factors) {
      final FactorScore factorScore = factor.score(profile);
      factorScores.put(factor.factorName(), factorScore);
      total += factorScore.getPoints();
    }

    final int clampedTotal = Math.max(0, Math.min(100, total));

    if (clampedTotal != total) {
      log.warn(
          "Composite credit score {} fell outside 0-100 before clamping — "
              + "check ScoringFactor implementations for weight misconfiguration.",
          total);
    }

    final RiskCategory riskCategory = RiskCategory.fromScore(clampedTotal);

    log.info(
        "Credit score computed: score={} riskCategory={} factors={}",
        clampedTotal,
        riskCategory,
        factorScores.keySet());

    return CreditScoreResult.builder()
        .score(clampedTotal)
        .riskCategory(riskCategory)
        .factorScores(factorScores)
        .build();
  }
}
