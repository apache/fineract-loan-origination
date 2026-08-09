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

package org.apache.fineract.los.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.los.domain.enums.ApprovalDecision;

/**
 * Input payload for recording a single approval-stage decision against a loan application.
 *
 * <p>{@code stageName} and {@code assignedOfficer} are deliberately absent. Both are derived
 * server-side — the stage from the application's current workflow position, the officer from the
 * authenticated principal — rather than trusted from client input. Accepting either from the
 * request body would let any authenticated caller record a decision under someone else's name or
 * against a stage they are not authorised to act on.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDecisionRequest {

  /** Approval decision to record. */
  @NotNull private ApprovalDecision decision;

  /**
   * Mandatory audit comments accompanying the approval decision.
   *
   * <p>Required for every workflow decision to provide a complete audit trail.
   */
  @NotBlank private String comments;
}
