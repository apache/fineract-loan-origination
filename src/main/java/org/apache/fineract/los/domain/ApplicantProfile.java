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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Stores personal and financial information about the loan applicant.
 *
 * <p>One-to-one relationship with {@code LoanApplication}. Separation of concerns — application
 * tracks lifecycle, profile tracks the person.
 *
 * <p>Financial fields (monthlyIncome, existingLoanObligations) feed directly into the credit
 * scoring engine:
 *
 * <ul>
 *   <li>Income-to-loan ratio uses monthlyIncome (30% weight)
 *   <li>Debt burden uses existingLoanObligations (25% weight)
 *   <li>Employment stability uses employmentStatus + employmentDurationMonths (20% weight)
 * </ul>
 */
@Entity
@Table(name = "applicant_profile")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ApplicantProfile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  /**
   * The loan application this profile belongs to. LAZY fetch — profile is not loaded unless
   * explicitly accessed.
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id", nullable = false, updatable = false)
  private LoanApplication application;

  /** Full legal name of the applicant. Must match identity document. */
  @Column(name = "full_name", nullable = false, length = 200)
  private String fullName;

  /**
   * National identity document number. Aadhaar (India), National ID (Kenya), Passport etc. Nullable
   * — some MFI environments use group guarantees instead of individual ID verification.
   */
  @Column(name = "national_id", length = 100)
  private String nationalId;

  /**
   * Monthly income in the application currency. Primary input for income-to-loan ratio scoring (30%
   * weight). Precision 19, scale 2 — matches Fineract monetary convention.
   */
  @Column(name = "monthly_income", precision = 19, scale = 2)
  private BigDecimal monthlyIncome;

  /**
   * Employment status of the applicant. Expected values: EMPLOYED, SELF_EMPLOYED, UNEMPLOYED,
   * INFORMAL. Used for employment stability scoring (20% weight).
   */
  @Column(name = "employment_status", length = 50)
  private String employmentStatus;

  /**
   * Number of months in current employment. Used for employment stability scoring (20% weight).
   * Higher duration → lower risk score.
   */
  @Column(name = "employment_duration_months")
  private Integer employmentDurationMonths;

  /**
   * Total existing loan obligations per month across all active loans. Primary input for debt
   * burden scoring (25% weight). Defaults to zero — no existing obligations.
   */
  @Column(name = "existing_loan_obligations", precision = 19, scale = 2)
  private BigDecimal existingLoanObligations = BigDecimal.ZERO;

  /**
   * The applicant's existing Fineract {@code clientId}. Required before the disbursement bridge can
   * call {@code POST /loans} — Fineract needs a pre-existing client record to attach the loan to.
   * Nullable during DRAFT/SUBMITTED/UNDER_REVIEW; the disbursement bridge fails fast with a clear
   * error if this is still null when the application reaches APPROVED.
   */
  @Column(name = "fineract_client_id")
  private Long fineractClientId;

  /** Tenant identifier — must match parent LoanApplication. */
  @Column(name = "tenant_id", nullable = false, length = 100, updatable = false)
  private String tenantId;

  /** Timestamp of profile creation. Set automatically by Spring Data Auditing. */
  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
