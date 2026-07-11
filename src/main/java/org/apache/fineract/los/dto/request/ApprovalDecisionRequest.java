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
 * <p>Consumed by {@code ApprovalWorkflowService#recordDecision}. The {@code comments} field is
 * validated by the service layer (mandatory for REJECT and REFER decisions).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDecisionRequest {

  /** Workflow stage name. Example: LOAN_OFFICER BRANCH_MANAGER CREDIT_COMMITTEE */
  @NotBlank private String stageName;

  /** Officer making the decision. */
  @NotBlank private String assignedOfficer;

  /** APPROVE / REJECT / REFER */
  @NotNull private ApprovalDecision decision;

  /** Optional comments. Required by the service layer for REJECT and REFER. */
  private String comments;
}
