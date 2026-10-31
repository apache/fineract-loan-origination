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

package org.apache.fineract.los.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.los.domain.enums.RiskCategory;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Stores the computed credit score and individual factor contributions for a loan application.
 *
 * One-to-one with {@code LoanApplication}. Computed automatically when the application enters
 * UNDER_REVIEW status. Immutable after creation.
 *
 * Individual factor scores are stored separately to support score explainability — a regulatory
 * requirement in many jurisdictions. Loan officers and applicants can see exactly which factors
 * drove the overall score.
 *
 * Factor weights (CGAP guidelines, configurable):
 *
 *
 *   Income-to-loan ratio: 30%
 *   Existing debt burden: 25%
 *   Employment stability: 20%
 *   Repayment history: 15%
 *   Loan purpose risk: 10%
 * 
 */
@Entity
@Table(name = "credit_score")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class CreditScore {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  /**
   * The loan application this score belongs to. LAZY fetch — score not loaded unless explicitly
   * accessed.
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id", nullable = false, updatable = false)
  private LoanApplication application;

  /**
   * Composite credit score — 0 to 100. Weighted sum of all factor scores. Higher is better — lower
   * risk.
   */
  @Column(name = "score", nullable = false)
  private Integer score;

  /**
   * Risk classification derived from the composite score. LOW (70-100), MEDIUM (40-69), HIGH
   * (0-39). Derived via {@code RiskCategory.fromScore(score)}.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "risk_category", nullable = false, length = 20)
  private RiskCategory riskCategory;

  /**
   * Score contribution from income-to-loan ratio. Max 30 points (30% weight). Higher monthly income
   * relative to requested amount → higher score.
   */
  @Column(name = "income_ratio_score")
  private Integer incomeRatioScore;

  /**
   * Score contribution from existing debt burden. Max 25 points (25% weight). Lower existing
   * obligations relative to income → higher score.
   */
  @Column(name = "debt_burden_score")
  private Integer debtBurdenScore;

  /**
   * Score contribution from employment stability. Max 20 points (20% weight). Stable employment +
   * longer duration → higher score.
   */
  @Column(name = "employment_score")
  private Integer employmentScore;

  /**
   * Score contribution from repayment history. Max 15 points (15% weight). Good past repayment
   * behavior in Fineract → higher score. Zero if no history exists (first-time borrower).
   */
  @Column(name = "repayment_history_score")
  private Integer repaymentHistoryScore;

  /**
   * Score contribution from loan purpose risk category. Max 10 points (10% weight). Low-risk
   * purposes (agriculture, education) → higher score. High-risk purposes (speculation) → lower
   * score.
   */
  @Column(name = "loan_purpose_score")
  private Integer loanPurposeScore;

  /**
   * Timestamp when scoring was computed. Set by the service layer at scoring time. Immutable after
   * creation.
   */
  @Column(name = "scored_at", nullable = false, updatable = false)
  private LocalDateTime scoredAt;

  /** Tenant identifier — must match parent LoanApplication. */
  @Column(name = "tenant_id", nullable = false, length = 100, updatable = false)
  private String tenantId;
}
