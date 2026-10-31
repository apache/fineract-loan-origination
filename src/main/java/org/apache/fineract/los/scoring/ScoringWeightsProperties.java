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

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalised configuration for credit scoring factor weights and thresholds.
 *
 * Bound from {@code application.yml} under the prefix {@code los.scoring.weights}. Institutions
 * can tune these values without recompiling — only a restart is required.
 *
 * Default values follow CGAP microfinance credit assessment guidelines and are applied if no
 * configuration is provided.
 *
 * {@link #validateWeightsSumToHundred()} runs on startup via {@code @PostConstruct} — fails fast
 * with a clear error if weights are misconfigured, rather than silently producing incorrect scores
 * at runtime.
 */
@Slf4j
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "los.scoring.weights")
public class ScoringWeightsProperties {

  // ─────────────────────────────────────────────────────────
  // Factor Weights
  // ─────────────────────────────────────────────────────────

  /** Weight for income-to-loan ratio factor. Default 30. */
  @Min(0)
  @Max(100)
  private int incomeRatio = 30;

  /** Weight for existing debt burden factor. Default 25. */
  @Min(0)
  @Max(100)
  private int debtBurden = 25;

  /** Weight for employment stability factor. Default 20. */
  @Min(0)
  @Max(100)
  private int employmentStability = 20;

  /** Weight for repayment history factor. Default 15. */
  @Min(0)
  @Max(100)
  private int repaymentHistory = 15;

  /** Weight for loan purpose risk factor. Default 10. */
  @Min(0)
  @Max(100)
  private int loanPurposeRisk = 10;

  // ─────────────────────────────────────────────────────────
  // Income Ratio Thresholds
  // ─────────────────────────────────────────────────────────

  /** Ratio at or above which income-ratio factor scores full points. */
  @DecimalMin("0.0")
  private BigDecimal incomeRatioFullThreshold = new BigDecimal("0.20");

  /** Ratio at or above which income-ratio factor scores 75%. */
  @DecimalMin("0.0")
  private BigDecimal incomeRatioHighThreshold = new BigDecimal("0.10");

  /** Ratio at or above which income-ratio factor scores 50%. */
  @DecimalMin("0.0")
  private BigDecimal incomeRatioMediumThreshold = new BigDecimal("0.05");

  // ─────────────────────────────────────────────────────────
  // Debt Burden Thresholds
  // ─────────────────────────────────────────────────────────

  /** Ratio at or below which debt-burden factor scores full points. */
  @DecimalMin("0.0")
  private BigDecimal debtBurdenLowThreshold = new BigDecimal("0.10");

  /** Ratio at or below which debt-burden factor scores 75%. */
  @DecimalMin("0.0")
  private BigDecimal debtBurdenModerateThreshold = new BigDecimal("0.30");

  /** Ratio at or below which debt-burden factor scores 50%. */
  @DecimalMin("0.0")
  private BigDecimal debtBurdenHighThreshold = new BigDecimal("0.50");

  /** Ratio at or below which debt-burden factor scores 25%. */
  @DecimalMin("0.0")
  private BigDecimal debtBurdenSevereThreshold = new BigDecimal("0.70");

  /**
   * Validates that all configured weights sum to exactly 100 on application startup.
   *
   * @throws IllegalStateException if weights do not sum to 100
   */
  @PostConstruct
  public void validateWeightsSumToHundred() {
    final int total =
        incomeRatio + debtBurden + employmentStability + repaymentHistory + loanPurposeRisk;

    if (total != 100) {
      throw new IllegalStateException(
          String.format(
              "Credit scoring weights must sum to 100 "
                  + "but summed to %d. Check "
                  + "los.scoring.weights.* configuration: "
                  + "income-ratio=%d, debt-burden=%d, "
                  + "employment-stability=%d, "
                  + "repayment-history=%d, "
                  + "loan-purpose-risk=%d",
              total,
              incomeRatio,
              debtBurden,
              employmentStability,
              repaymentHistory,
              loanPurposeRisk));
    }

    log.info(
        "Credit scoring weights validated successfully: "
            + "incomeRatio={} debtBurden={} "
            + "employmentStability={} "
            + "repaymentHistory={} loanPurposeRisk={}",
        incomeRatio,
        debtBurden,
        employmentStability,
        repaymentHistory,
        loanPurposeRisk);
  }
}
