import { LoanApplicationStatus, LoanPurpose, EmploymentStatus } from './enums';

export interface ApplicantDetails {
  fullName: string;
  nationalId?: string;
  monthlyIncome?: number;
  employmentStatus?: EmploymentStatus;
  employmentDurationMonths?: number;
  existingLoanObligations?: number;
  fineractClientId?: number;
}

export interface CreateLoanApplicationRequest {
  requestedAmount: number;
  currency?: string;
  loanPurpose?: LoanPurpose;
  tenorMonths?: number;
  fineractLoanProductId?: number;
  applicant: ApplicantDetails;
}

export interface LoanApplication {
  applicationRef: string;
  status: LoanApplicationStatus;
  requestedAmount: number;
  currency: string;
  loanPurpose: string;
  tenorMonths: number;
  fineractLoanProductId: number | null;
  fineractLoanId: number | null;
  createdAt: string;
  updatedAt: string;
}