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
 * Represents the lifecycle state of a RequiredDocument attached to a LoanApplication.
 *
 * 
 * PENDING
 *   └─► UPLOADED
 *         ├─► VERIFIED
 *         └─► REJECTED
 *               └─► PENDING (re-upload allowed)
 * 
 */
public enum DocumentStatus {

  /** Document is required but has not yet been uploaded by the applicant. */
  PENDING,

  /** Document has been uploaded and is awaiting verification by a loan officer. */
  UPLOADED,

  /** Document has been verified and accepted by a loan officer. */
  VERIFIED,

  /** Document was rejected — wrong type, unreadable, or expired. Applicant must re-upload. */
  REJECTED;

  /** Returns true if the document has been accepted for processing. */
  public boolean isAccepted() {
    return this == VERIFIED;
  }

  /** Returns true if the document still requires action from the applicant. */
  public boolean requiresApplicantAction() {
    return this == PENDING || this == REJECTED;
  }

  @Override
  public String toString() {
    return this.name();
  }
}
