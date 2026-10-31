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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.los.domain.enums.ApprovalDecision;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Records a single approval decision at one stage of the multi-stage approval workflow.
 *
 * Many-to-one with {@code LoanApplication}. One application accumulates multiple approval stage
 * records as it progresses through the workflow — full audit trail.
 *
 * <p>This entity is immutable after creation — once a decision is recorded it must not be modified.
 * If a stage needs to be reconsidered, a new ApprovalStage record is created.
 *
 * <p>Required for regulatory compliance — auditors can reconstruct the full approval history of any
 * loan application from this table.
 */
@Entity
@Table(name = "approval_stage")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ApprovalStage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  /**
   * The loan application this approval stage belongs to. LAZY fetch — stages not loaded unless
   * explicitly accessed.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id", nullable = false, updatable = false)
  private LoanApplication application;

  /**
   * Name of this approval stage in the workflow. Standard values: LOAN_OFFICER, BRANCH_MANAGER,
   * CREDIT_COMMITTEE. Configurable per institution via application.properties.
   */
  @Column(name = "stage_name", nullable = false, length = 100, updatable = false)
  private String stageName;

  /**
   * Name or identifier of the officer assigned to this stage. Populated from the authenticated
   * user's JWT claims. Stored for audit trail — who was responsible.
   */
  @Column(name = "assigned_officer", length = 200, updatable = false)
  private String assignedOfficer;

  /**
   * Decision made at this stage. Null until the officer acts on the application. Maps to
   * LoanApplicationStatus transition: APPROVE → toward APPROVED REJECT → REJECTED (immediate
   * terminal) REFER → REFERRED
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "decision", length = 30)
  private ApprovalDecision decision;

  /**
   * Officer comments explaining the decision. Mandatory when decision is REJECT or REFER — enforced
   * at service layer, not database level. Stored as TEXT — no length limit.
   */
  @Column(name = "comments", columnDefinition = "TEXT")
  private String comments;

  /**
   * Timestamp when the officer made the decision. Null until decision is recorded. Set by the
   * service layer at decision time.
   */
  @Column(name = "decided_at", updatable = false)
  private LocalDateTime decidedAt;

  /** Tenant identifier — must match parent LoanApplication. */
  @Column(name = "tenant_id", nullable = false, length = 100, updatable = false)
  private String tenantId;

  /**
   * Timestamp of stage record creation. Set automatically by Spring Data Auditing. Distinct from
   * decidedAt — stage may be created (assigned) before the officer makes a decision.
   */
  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
