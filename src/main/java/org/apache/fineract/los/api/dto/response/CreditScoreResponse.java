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

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.apache.fineract.los.domain.CreditScore;
import org.apache.fineract.los.domain.enums.RiskCategory;

/** API-facing view of a {@link CreditScore}. */
@Getter
@Builder
public class CreditScoreResponse {

  private final int score;
  private final RiskCategory riskCategory;
  private final int incomeRatioScore;
  private final int debtBurdenScore;
  private final int employmentScore;
  private final int repaymentHistoryScore;
  private final int loanPurposeScore;
  private final LocalDateTime scoredAt;

  public static CreditScoreResponse from(final CreditScore score) {
    return CreditScoreResponse.builder()
        .score(score.getScore())
        .riskCategory(score.getRiskCategory())
        .incomeRatioScore(score.getIncomeRatioScore())
        .debtBurdenScore(score.getDebtBurdenScore())
        .employmentScore(score.getEmploymentScore())
        .repaymentHistoryScore(score.getRepaymentHistoryScore())
        .loanPurposeScore(score.getLoanPurposeScore())
        .scoredAt(score.getScoredAt())
        .build();
  }
}