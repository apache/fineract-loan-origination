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
 * Thrown when the same officer attempts to record a second approval decision on an application —
 * violates the four-eyes principle enforced by {@code ApprovalWorkflowService}.
 *
 * <p>Unchecked — the global exception handler maps this to HTTP 409 Conflict.
 */
public class DuplicateApprovalException extends RuntimeException {

  public DuplicateApprovalException(final String applicationRef, final String assignedOfficer) {
    super(
        String.format(
            LosErrorConstants.MSG_DUPLICATE_APPROVAL_TEMPLATE, assignedOfficer, applicationRef));
  }
}
