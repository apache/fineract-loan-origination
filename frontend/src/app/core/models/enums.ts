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
// Mirrors org.apache.fineract.los.domain.enums.LoanApplicationStatus exactly.
// DRAFT -> SUBMITTED -> UNDER_REVIEW -> APPROVED -> DISBURSED
//                                    -> REJECTED (terminal)
//                                    -> REFERRED -> UNDER_REVIEW
export type LoanApplicationStatus =
  'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'REFERRED' | 'DISBURSED';

// Mirrors org.apache.fineract.los.domain.enums.LoanPurpose exactly.
export type LoanPurpose =
  | 'AGRICULTURE'
  | 'EDUCATION'
  | 'BUSINESS'
  | 'HOME_IMPROVEMENT'
  | 'CONSUMER'
  | 'MEDICAL'
  | 'SPECULATION'
  | 'OTHER';

// Mirrors org.apache.fineract.los.domain.enums.EmploymentStatus exactly.
export type EmploymentStatus = 'EMPLOYED' | 'SELF_EMPLOYED' | 'INFORMAL' | 'UNEMPLOYED';

// Mirrors org.apache.fineract.los.domain.enums.RiskCategory exactly.
export type RiskCategory = 'LOW' | 'MEDIUM' | 'HIGH';

// Mirrors org.apache.fineract.los.domain.enums.ApprovalDecision exactly.
// Note: REFER exists on the backend — not just APPROVE/REJECT.
export type ApprovalDecisionType = 'APPROVE' | 'REJECT' | 'REFER';

// Confirmed via ApprovalWorkflowProperties / your yml: los.workflow.stages.
// These are configured values, not a closed backend enum, but this is the
// current fixed set — update here if the yml list changes.
export type ApprovalStageName = 'LOAN_OFFICER' | 'BRANCH_MANAGER' | 'CREDIT_COMMITTEE';

// Mirrors org.apache.fineract.los.domain.enums.DocumentStatus exactly.
// No document upload endpoint exists yet on the backend (RequiredDocumentRepository
// exists but no controller) — this is here for when that lands.
export type DocumentStatus = 'PENDING' | 'UPLOADED' | 'VERIFIED' | 'REJECTED';
