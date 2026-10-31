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
 * Represents the decision made by an approving officer at a specific {@code ApprovalStage}.
 *
 * Each decision maps directly to a {@code LoanApplicationStatus} transition:
 *
 * 
 *   APPROVE → drives toward APPROVED
 *   REJECT → drives toward REJECTED
 *   REFER → drives toward REFERRED
 * 
 */
public enum ApprovalDecision {

  /**
   * Officer approves this stage. If this is the final stage, application moves to APPROVED status.
   */
  APPROVE,

  /**
   * Officer rejects the application outright. Application moves to REJECTED status immediately.
   * Terminal — no further approval possible.
   */
  REJECT,

  /**
   * Officer refers the application back to the applicant for additional information or documents.
   * Application moves to REFERRED status.
   */
  REFER;

  /** Returns true if this decision results in the application being closed permanently. */
  public boolean isTerminating() {
    return this == REJECT;
  }

  /** Returns true if this decision allows the application to continue in the workflow. */
  public boolean allowsContinuation() {
    return this == APPROVE || this == REFER;
  }

  @Override
  public String toString() {
    return this.name();
  }
}
