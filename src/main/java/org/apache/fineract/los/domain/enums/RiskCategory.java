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

package org.apache.fineract.los.domain.enums;

/**
 * Represents the credit risk classification of a loan applicant as determined by the {@code
 * CreditScoringStrategy}.
 *
 * Score ranges follow CGAP microfinance credit assessment guidelines. Thresholds are
 * configurable via {@code application.properties} — these are defaults only.
 *
 * 
 * Score 70-100 → LOW
 * Score 40-69  → MEDIUM
 * Score 0-39   → HIGH
 * 
 */
public enum RiskCategory {

  /**
   * Low risk applicant. Score 70-100. Strong income ratio, stable employment, low existing debt
   * burden.
   */
  LOW(70, 100),

  /**
   * Medium risk applicant. Score 40-69. Acceptable profile with some risk factors. May require
   * additional scrutiny at approval stage.
   */
  MEDIUM(40, 69),

  /**
   * High risk applicant. Score 0-39. Significant risk factors present. Likely to be referred or
   * rejected.
   */
  HIGH(0, 39);

  private final int minScore;
  private final int maxScore;

  RiskCategory(int minScore, int maxScore) {
    this.minScore = minScore;
    this.maxScore = maxScore;
  }

  public int getMinScore() {
    return minScore;
  }

  public int getMaxScore() {
    return maxScore;
  }

  /**
   * Derives the RiskCategory from a numeric score. Scores outside 0-100 are clamped to HIGH.
   *
   * @param score numeric credit score 0-100
   * @return the corresponding RiskCategory
   */
  public static RiskCategory fromScore(int score) {
    for (RiskCategory category : values()) {
      if (score >= category.minScore && score <= category.maxScore) {
        return category;
      }
    }
    return HIGH;
  }

  /** Returns true if this category requires elevated scrutiny during approval. */
  public boolean requiresElevatedScrutiny() {
    return this == HIGH || this == MEDIUM;
  }

  @Override
  public String toString() {
    return this.name();
  }
}
