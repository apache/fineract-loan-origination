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

package org.apache.fineract.los.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;

/**
 * API-facing view of a {@link LoanApplication}.
 *
 * <p>Deliberately decoupled from the JPA entity so the wire format doesn't silently change every
 * time the schema does.
 */
@Getter
@Builder
public class LoanApplicationResponse {

  private final String applicationRef;
  private final LoanApplicationStatus status;
  private final BigDecimal requestedAmount;
  private final String currency;
  private final String loanPurpose;
  private final Integer tenorMonths;
  private final Long fineractLoanProductId;
  private final Long fineractLoanId;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  public static LoanApplicationResponse from(final LoanApplication app) {
    return LoanApplicationResponse.builder()
        .applicationRef(app.getApplicationRef())
        .status(app.getStatus())
        .requestedAmount(app.getRequestedAmount())
        .currency(app.getCurrency())
        .loanPurpose(app.getLoanPurpose())
        .tenorMonths(app.getTenorMonths())
        .fineractLoanProductId(app.getFineractLoanProductId())
        .fineractLoanId(app.getFineractLoanId())
        .createdAt(app.getCreatedAt())
        .updatedAt(app.getUpdatedAt())
        .build();
  }
}