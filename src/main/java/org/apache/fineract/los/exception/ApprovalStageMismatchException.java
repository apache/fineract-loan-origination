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
package org.apache.fineract.los.exception;

/**
 * Thrown when the authenticated staff member's LOS workflow role does not match the approval stage
 * an application is currently sitting at — e.g. a BRANCH_MANAGER attempting to act while the
 * application is still awaiting its LOAN_OFFICER decision.
 *
 * <p>Unchecked — the global exception handler maps this to HTTP 409 Conflict.
 */
public class ApprovalStageMismatchException extends RuntimeException {

  public ApprovalStageMismatchException(
      final String applicationRef, final String expectedStage, final String assignedOfficer) {
    super(
        String.format(
            LosErrorConstants.MSG_STAGE_MISMATCH_TEMPLATE,
            assignedOfficer,
            applicationRef,
            expectedStage));
  }
}
