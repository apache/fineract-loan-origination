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

package org.apache.fineract.los.scoring.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Result of a single scoring factor's evaluation.
 *
 * Each {@code ScoringFactor} produces one of these, carrying both the raw contribution to the
 * composite score and a human-readable explanation. The explanation is stored and surfaced to
 * applicants — required for regulatory explainability in most lending jurisdictions.
 */
@Getter
@Builder
public class FactorScore {

  /**
   * Points contributed by this factor toward the composite score. Always between 0 and the factor's
   * configured weight — e.g. income ratio factor contributes 0-30 points if its weight is
   * configured as 30.
   */
  private final int points;

  /**
   * Maximum possible points for this factor, equal to its configured weight. Used by callers to
   * compute percentage contribution (points / maxPoints) for display purposes.
   */
  private final int maxPoints;

  /**
   * Human-readable explanation of why this score was given. Example: "Income-to-loan ratio of 3.2x
   * indicates strong repayment capacity."
   */
  private final String explanation;
}
