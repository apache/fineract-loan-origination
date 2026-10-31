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

package org.apache.fineract.los.statemachine;

import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.exception.LosErrorConstants;

/**
 * Thrown when an invalid state transition is attempted on a {@link
 * org.apache.fineract.los.domain.LoanApplication}.
 *
 * This is an unchecked exception — callers are not required to catch it, but the global
 * exception handler maps it to HTTP 409 Conflict so clients receive a meaningful error.
 *
 * Example: attempting DRAFT → DISBURSED directly will throw this exception with a descriptive
 * message identifying both the current and attempted target status.
 */
public class LoanStateTransitionException extends RuntimeException {

  /** Current status of the application at time of error. */
  private final LoanApplicationStatus fromStatus;

  /** Target status that was attempted illegally. */
  private final LoanApplicationStatus toStatus;

  /**
   * Constructs a new transition exception with context.
   *
   * @param fromStatus current application status
   * @param toStatus attempted target status
   */
  public LoanStateTransitionException(
      final LoanApplicationStatus fromStatus, final LoanApplicationStatus toStatus) {

    super(String.format(LosErrorConstants.MSG_INVALID_TRANSITION_TEMPLATE, fromStatus, toStatus));

    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
  }

  /**
   * Returns the status the application was in when the invalid transition was attempted.
   *
   * @return current status at time of error
   */
  public LoanApplicationStatus getFromStatus() {
    return fromStatus;
  }

  /**
   * Returns the target status that was illegally attempted.
   *
   * @return attempted target status
   */
  public LoanApplicationStatus getToStatus() {
    return toStatus;
  }
}
