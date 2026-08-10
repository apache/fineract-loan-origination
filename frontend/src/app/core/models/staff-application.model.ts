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
import {
  ApprovalDecisionType,
  ApprovalStageName,
  LoanApplicationStatus,
  RiskCategory,
} from './enums';

export interface StaffCreditScore {
  score: number;
  riskRating: RiskCategory;
  incomeRatioScore: number;
  debtBurdenScore: number;
  employmentScore: number;
  repaymentHistoryScore: number;
  loanPurposeScore: number;
  scoredAt: string;
}

export interface StaffApprovalStage {
  stage: ApprovalStageName;
  decision: ApprovalDecisionType | null;
  decidedBy: string;
  decidedAt: string | null;
  notes: string | null;
}

export interface StaffApplicationDetail {
  applicationRef: string;
  status: LoanApplicationStatus;
  requestedAmount: number;
  currency: string;
  loanPurpose: string;
  tenorMonths: number;
  submittedAt: string;
  updatedAt: string;
  // Applicant
  applicantName: string;
  nationalId: string | null;
  fineractClientId: number | null;
  monthlyIncome: number | null;
  employmentStatus: string | null;
  employmentDurationMonths: number | null;
  existingLoanObligations: number | null;
  // Related
  creditScore: StaffCreditScore | null;
  approvalStages: StaffApprovalStage[];
  fineractLoanId: number | null;
  disbursedAt: string | null;
}

export interface StaffApplicationSummary {
  applicationRef: string;
  status: LoanApplicationStatus;
  requestedAmount: number;
  currency: string;
  loanPurpose: string;
  tenorMonths: number;
  createdAt: string;
  updatedAt: string;
  fineractLoanId: number | null;
}

export interface ApprovalDecisionRequest {
  decision: ApprovalDecisionType;
  comments: string;
}
