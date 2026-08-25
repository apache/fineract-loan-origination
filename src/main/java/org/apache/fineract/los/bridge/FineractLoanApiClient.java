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

package org.apache.fineract.los.bridge;

import org.apache.fineract.los.bridge.dto.FineractLoanCreateRequest;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateResponse;
import org.apache.fineract.los.bridge.dto.FineractLoanRequest;
import org.apache.fineract.los.bridge.dto.FineractLoanResponse;

/**
 * Adapter boundary between the LOS disbursement bridge and Apache Fineract's core REST API.
 *
 * <p>Exactly one implementation is active at runtime, selected via {@code
 * los.fineract.mock-enabled}:
 *
 * <ul>
 *   <li>{@link RestFineractLoanApiClient} — calls a real Fineract instance's loan APIs
 *   <li>{@link MockFineractLoanApiClient} — returns simulated responses for the full loan lifecycle
 * </ul>
 *
 * <p>{@code DisbursementBridgeService} depends only on this interface — swapping implementations
 * requires no change to calling code, satisfying the "bridge isolated behind an interface" risk
 * mitigation in the proposal.
 *
 * <p>Fineract loan lifecycle operations:
 *
 * <ol>
 *   <li>{@link #createLoan} — POST /loans — creates the loan, returns loanId
 *   <li>{@link #approveLoan} — POST /loans/{loanId}?command=approve — approves the loan
 *   <li>{@link #disburseLoan} — POST /loans/{loanId}?command=disburse — disburses funds
 * </ol>
 */
public interface FineractLoanApiClient {

  /**
   * Creates a loan in Fineract for an approved LOS application.
   *
   * @param request the loan creation payload
   * @return the Fineract response identifying the created loan
   */
  FineractLoanCreateResponse createLoan(FineractLoanCreateRequest request);

  /**
   * Approves a Fineract loan that was previously created.
   *
   * @param loanId the Fineract loan identifier
   * @param request the approval payload containing approval date and optional parameters
   * @return the Fineract response confirming approval
   */
  FineractLoanResponse approveLoan(Long loanId, FineractLoanRequest request);

  /**
   * Disburses a Fineract loan that was previously approved.
   *
   * @param loanId the Fineract loan identifier
   * @param request the disbursement payload containing disbursement date and optional parameters
   * @return the Fineract response confirming disbursement
   */
  FineractLoanResponse disburseLoan(Long loanId, FineractLoanRequest request);
}
