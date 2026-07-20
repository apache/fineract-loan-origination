import { RiskCategory } from './enums';

/** Mirrors CreditScoreResponse.java exactly. */
export interface CreditScore {
  score: number;
  riskCategory: RiskCategory;
  incomeRatioScore: number;
  debtBurdenScore: number;
  employmentScore: number;
  repaymentHistoryScore: number;
  loanPurposeScore: number;
  scoredAt: string;
}