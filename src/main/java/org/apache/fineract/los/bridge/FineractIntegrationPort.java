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

import org.apache.fineract.los.bridge.dto.FineractLoanRequest;
import org.apache.fineract.los.bridge.dto.FineractLoanResponse;

/**
 * Port (interface) for integration with Apache Fineract core.
 *
 * <p>Two implementations exist:
 * <ul>
 *   <li>{@code MockFineractAdapter} — active on {@code dev}
 *       profile, returns simulated responses without calling
 *       any external system. Enables full end-to-end demo
 *       without a running Fineract instance.</li>
 *   <li>{@code RealFineractAdapter} — active on {@code prod}
 *       profile, calls Fineract's {@code POST /loans} API
 *       with the exact payload Fineract expects.</li>
 * </ul>
 *
 * <p>Switching between implementations requires only changing
 * the active Spring profile — no business logic changes.
 *
 * <p>The correlation ID from {@code X-Correlation-Id} is
 * forwarded on every outbound call so LOS and Fineract logs
 * can be joined during debugging (FINERACT-1656).
 */
public interface FineractIntegrationPort {

    /**
     * Creates a loan in Apache Fineract for an approved
     * loan application.
     *
     * @param request payload constructed from the approved
     *                LoanApplication and ApplicantProfile
     * @return response containing the Fineract loan ID
     * @throws FineractIntegrationException if the call fails
     */
    FineractLoanResponse createLoan(FineractLoanRequest request);
}