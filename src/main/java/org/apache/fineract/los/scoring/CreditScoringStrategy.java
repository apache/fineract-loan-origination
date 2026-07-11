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

package org.apache.fineract.los.scoring;

import org.apache.fineract.los.scoring.model.ApplicantScoringProfile;
import org.apache.fineract.los.scoring.model.CreditScoreResult;

/**
 * Top-level strategy for computing a composite credit score from an {@link
 * ApplicantScoringProfile}.
 *
 * <p>The default implementation ({@link DefaultCreditScoringStrategy}) aggregates all registered
 * {@link ScoringFactor} beans using the rule-based weighting described in the GSoC proposal. Future
 * contributors can swap in a credit-bureau-backed or ML-backed strategy by implementing this
 * interface and marking it {@code @Primary} — no changes required to {@code CreditScoringService}
 * or the entity model.
 */
public interface CreditScoringStrategy {

  /**
   * Computes a composite credit score for the given applicant profile.
   *
   * @param profile immutable input data for scoring
   * @return composite result including per-factor breakdown, never null
   */
  CreditScoreResult calculate(ApplicantScoringProfile profile);
}
