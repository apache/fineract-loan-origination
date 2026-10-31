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

package org.apache.fineract.los.scoring.model;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import org.apache.fineract.los.domain.enums.RiskCategory;

/**
 * Composite output of the credit scoring engine.
 *
 * Deliberately decoupled from the {@code CreditScore} JPA entity — the service layer maps this
 * result onto the entity for persistence. Keeping the scoring engine free of persistence concerns
 * allows it to be tested and evolved independently.
 *
 * {@link #factorScores} preserves individual factor contributions for explainability — both the
 * applicant-facing score breakdown and regulatory audit reporting depend on this granularity being
 * available, not just the final number.
 */
@Getter
@Builder
public class CreditScoreResult {

  /** Composite score from 0 to 100. */
  private final int score;

  /** Risk classification derived from the composite score. */
  private final RiskCategory riskCategory;

  /**
   * Individual factor contributions keyed by factor name, e.g. "income-ratio", "debt-burden".
   * Preserves order of evaluation via {@link java.util.LinkedHashMap} at construction time in the
   * strategy implementation.
   */
  private final Map<String, FactorScore> factorScores;
}
