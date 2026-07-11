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

/**
 * Adapter boundary between the LOS disbursement bridge and Apache Fineract's core REST API.
 *
 * <p>Exactly one implementation is active at runtime, selected via {@code
 * los.fineract.mock-enabled}:
 *
 * <ul>
 *   <li>{@link RestFineractLoanApiClient} — calls a real Fineract instance's {@code POST /loans}
 *   <li>{@link MockFineractLoanApiClient} — returns a simulated response, used while FINERACT-2418
 *       is unresolved so the full origination flow remains demonstrable
 * </ul>
 *
 * <p>{@code DisbursementBridgeService} depends only on this interface — swapping implementations
 * requires no change to calling code, satisfying the "bridge isolated behind an interface" risk
 * mitigation in the proposal.
 */
public interface FineractLoanApiClient {

  /**
   * Creates a loan in Fineract for an approved application.
   *
   * @param request the loan creation payload
   * @return the Fineract response identifying the created loan
   */
  FineractLoanCreateResponse createLoan(FineractLoanCreateRequest request);
}
