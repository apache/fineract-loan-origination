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

package org.apache.fineract.los.bridge.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/**
 * Generic request payload for Fineract loan command operations.
 *
 * <p>Used for approve and disburse commands. Field names match Fineract's JSON contract exactly.
 */
@Getter
@Builder
public class FineractLoanRequest {

  // ── Approval fields ──

  /** Approval date. Format: dd MMMM yyyy. Required for approve command. */
  private final String approvedOnDate;

  /** Optional override of approved loan amount. */
  private final BigDecimal approvedLoanAmount;

  /** Optional override of expected disbursement date during approval. */
  private final String expectedDisbursementDate;

  // ── Disbursement fields ──

  /** Actual disbursement date. Format: dd MMMM yyyy. Required for disburse command. */
  private final String actualDisbursementDate;

  /** Optional override of transaction amount during disbursement. */
  private final BigDecimal transactionAmount;

  /** Optional fixed EMI amount. */
  private final BigDecimal fixedEmiAmount;

  // ── Common fields ──

  /** Locale for date formatting. */
  private final String locale;

  /** Date format pattern. */
  private final String dateFormat;

  /** Optional notes/comments. */
  private final String note;
}
