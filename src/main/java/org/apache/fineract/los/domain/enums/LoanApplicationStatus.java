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
 * Represents the lifecycle states of a LoanApplication.
 *
 * Valid transitions are enforced exclusively by {@code LoanOriginationStateMachine}. No code
 * outside the state machine may call {@code LoanApplication#setStatus} directly.
 *
 * 
 * DRAFT
 *   └─► SUBMITTED
 *         └─► UNDER_REVIEW
 *               ├─► APPROVED
 *               │     └─► DISBURSED
 *               ├─► REJECTED
 *               └─► REFERRED
 *                     └─► UNDER_REVIEW
 * 
 */
public enum LoanApplicationStatus {

  /** Application has been started but not yet submitted. All fields are editable in this state. */
  DRAFT,

  /**
   * Application has been submitted by the applicant. No further edits allowed. Awaiting loan
   * officer pickup.
   */
  SUBMITTED,

  /**
   * Application is actively being reviewed by a loan officer. Credit scoring runs automatically on
   * entry to this state.
   */
  UNDER_REVIEW,

  /**
   * All approval stages completed with an APPROVE decision. Disbursement bridge will be triggered
   * automatically.
   */
  APPROVED,

  /**
   * Application was rejected at any approval stage. Terminal state — no further transitions
   * allowed.
   */
  REJECTED,

  /**
   * Application referred back to applicant for additional information or document resubmission.
   * Transitions back to UNDER_REVIEW on resubmission.
   */
  REFERRED,

  /** Loan successfully created in Apache Fineract via the disbursement bridge. Terminal state. */
  DISBURSED;

  /** Returns true if this status is a terminal state — no further transitions are possible. */
  public boolean isTerminal() {
    return this == REJECTED || this == DISBURSED;
  }

  /** Returns true if this status allows applicant to edit the application. */
  public boolean isEditable() {
    return this == DRAFT;
  }

  @Override
  public String toString() {
    return this.name();
  }
}
