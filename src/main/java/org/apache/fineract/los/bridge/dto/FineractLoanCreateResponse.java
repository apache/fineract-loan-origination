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

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload returned by Apache Fineract's {@code POST /loans} endpoint.
 *
 * <p>Mirrors Fineract's {@code CommandProcessingResult} shape: {@code
 * {"officeId":1,"clientId":5,"loanId":42,"resourceId":42}}. Mutable with a no-args constructor so
 * Jackson can deserialise it directly from the Fineract response body; {@code
 * MockFineractLoanApiClient} uses the builder instead.
 */
@Getter
@Setter
@NoArgsConstructor
@Builder
public class FineractLoanCreateResponse {

  private Long officeId;
  private Long clientId;
  private Long loanId;
  private Long resourceId;

  public FineractLoanCreateResponse(
      final Long officeId, final Long clientId, final Long loanId, final Long resourceId) {
    this.officeId = officeId;
    this.clientId = clientId;
    this.loanId = loanId;
    this.resourceId = resourceId;
  }
}
