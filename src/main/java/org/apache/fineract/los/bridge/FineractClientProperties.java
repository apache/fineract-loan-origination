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

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised configuration for the connection to Apache Fineract's core REST API, used by the
 * disbursement bridge.
 *
 * <p>Bound from {@code application.yml} under the prefix {@code los.fineract}.
 *
 * <p>Per the GSoC proposal's alignment strategy: while FINERACT-2418 is unresolved, {@code
 * mockEnabled} defaults to {@code true} so the full origination flow can be demonstrated end-to-end
 * without a live Fineract instance. Once real endpoints are available, institutions flip this
 * single flag — no code changes required.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "los.fineract")
public class FineractClientProperties {

  /** Base URL of the Fineract core instance, e.g. https://localhost/fineract-provider/api/v1. */
  private String baseUrl = "https://localhost/fineract-provider/api/v1";

  /** Fineract tenant identifier sent via the X-Fineract-Platform-TenantId header. */
  private String tenantId = "default";

  /** Basic-auth username used to call Fineract. */
  private String username = "mifos";

  /** Basic-auth password used to call Fineract. */
  private String password = "password";

  /**
   * When true, the disbursement bridge uses {@code MockFineractLoanApiClient} instead of calling a
   * real Fineract instance. Defaults to true so the POC is demonstrable without a live dependency
   * on FINERACT-2418.
   */
  private boolean mockEnabled = true;

  /** Connect timeout in milliseconds for calls to Fineract. */
  private int connectTimeoutMs = 5000;

  /** Read timeout in milliseconds for calls to Fineract. */
  private int readTimeoutMs = 10000;

  /** Default transactionProcessingStrategyCode sent on loan creation. */
  private String defaultTransactionProcessingStrategyCode = "1";

  /** Default loanTermFrequencyType / repaymentFrequencyType (Fineract enum: 2 = Months). */
  private String defaultFrequencyType = "2";

  /** Default number of repayment periods between each repayment. */
  private int defaultRepaymentEvery = 1;

  /** Default nominal interest rate per repayment period, applied when none is configured. */
  private String defaultInterestRatePerPeriod = "12";

  /** Fineract locale parameter required on most write endpoints. */
  private String locale = "en";

  /** Fineract dateFormat parameter required on most write endpoints. */
  private String dateFormat = "dd MMMM yyyy";
}
