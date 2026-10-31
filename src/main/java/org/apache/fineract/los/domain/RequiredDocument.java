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
import org.apache.fineract.los.domain.enums.DocumentStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Tracks a document required for a loan application.
 *
 * Many-to-one relationship with {@code LoanApplication}. One application may require multiple
 * documents — ID proof, income proof, bank statement etc.
 *
 * The state machine uses this entity to enforce that all required documents are VERIFIED before
 * an application can transition from SUBMITTED to UNDER_REVIEW.
 */
@Entity
@Table(name = "required_document")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class RequiredDocument {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  /**
   * The loan application this document belongs to. LAZY fetch — documents not loaded unless
   * explicitly accessed.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id", nullable = false, updatable = false)
  private LoanApplication application;

  /**
   * Type of document required. Examples: ID_PROOF, INCOME_PROOF, BANK_STATEMENT, COLLATERAL_PROOF,
   * GUARANTOR_ID. Institutions configure their required document types via application.properties.
   */
  @Column(name = "document_type", nullable = false, length = 100)
  private String documentType;

  /**
   * Current status of this document in the verification pipeline. Defaults to PENDING — document
   * required but not yet uploaded. Transitions: PENDING → UPLOADED → VERIFIED or REJECTED.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "document_status", nullable = false, length = 30)
  private DocumentStatus documentStatus = DocumentStatus.PENDING;

  /**
   * Original filename of the uploaded document. Null until the applicant uploads the document.
   * Stored for audit and retrieval purposes only — actual file storage is handled by the document
   * storage adapter (local filesystem in dev, Azure Blob in prod).
   */
  @Column(name = "file_name", length = 255)
  private String fileName;

  /** Timestamp when the document was uploaded by the applicant. Null until upload occurs. */
  @Column(name = "uploaded_at")
  private LocalDateTime uploadedAt;

  /** Tenant identifier — must match parent LoanApplication. */
  @Column(name = "tenant_id", nullable = false, length = 100, updatable = false)
  private String tenantId;

  /** Timestamp of record creation. Set automatically by Spring Data Auditing. */
  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
