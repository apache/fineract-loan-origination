// Mirrors org.apache.fineract.los.domain.enums.LoanApplicationStatus exactly.
// DRAFT -> SUBMITTED -> UNDER_REVIEW -> APPROVED -> DISBURSED
//                                    -> REJECTED (terminal)
//                                    -> REFERRED -> UNDER_REVIEW
export type LoanApplicationStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'REFERRED'
  | 'DISBURSED';

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