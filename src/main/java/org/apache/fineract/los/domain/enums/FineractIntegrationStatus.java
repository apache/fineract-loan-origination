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
 * Tracks the integration state with Apache Fineract for disbursement lifecycle orchestration.
 *
 * <p>Prevents duplicate Fineract operations and enables idempotent retry logic. The LOS application
 * status (APPROVED/DISBURSED) and the Fineract integration status are intentionally decoupled:
 *
 * <ul>
 *   <li>LOS status tracks the origination workflow state
 *   <li>Fineract integration status tracks the external system synchronization state
 * </ul>
 *
 * <p>This separation allows:
 *
 * <ul>
 *   <li>Recovery from partial failures (e.g., loan created but approval failed)
 *   <li>Clear visibility into which Fineract operation failed
 *   <li>Safe retry of failed operations without duplicate loan creation
 * </ul>
 *
 * <p>Lifecycle progression:
 *
 * <pre>
 * null (not started)
 *   ↓ (POST /loans succeeds)
 * LOAN_CREATED
 *   ↓ (POST /loans/{id}?command=approve succeeds)
 * LOAN_APPROVED
 *   ↓ (POST /loans/{id}?command=disburse succeeds)
 * LOAN_DISBURSED
 * </pre>
 *
 * <p>FAILED state is set when any operation fails and can be retried from the last successful
 * state.
 */
public enum FineractIntegrationStatus {

  /** Fineract loan has been created via POST /loans. fineractLoanId is populated. */
  LOAN_CREATED,

  /** Fineract loan has been approved via POST /loans/{id}?command=approve. */
  LOAN_APPROVED,

  /** Fineract loan has been disbursed via POST /loans/{id}?command=disburse. Terminal state. */
  LOAN_DISBURSED,

  /**
   * A Fineract integration operation failed. The application can be retried from the last
   * successful state.
   */
  FAILED;

  /**
   * Returns true if this is a terminal integration state — no further Fineract operations are
   * needed.
   */
  public boolean isTerminal() {
    return this == LOAN_DISBURSED;
  }

  @Override
  public String toString() {
    return this.name();
  }
}
