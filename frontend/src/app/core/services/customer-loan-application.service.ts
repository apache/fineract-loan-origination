import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateLoanApplicationRequest, LoanApplication } from '../models';

const BASE_URL = `${environment.losApiUrl}/customer/loan-applications`;

/**
 * Customer-facing counterpart to LoanApplicationService. Staff/back-office actions
 * (start-review, approval decisions, disbursement) live only in LoanApplicationService —
 * a customer can create, view, and submit their own applications, but cannot move them
 * through the internal review workflow.
 */
@Injectable({ providedIn: 'root' })
export class CustomerLoanApplicationService {
  private readonly http = inject(HttpClient);

  /** POST /api/v1/customer/loan-applications */
  create(request: CreateLoanApplicationRequest): Observable<LoanApplication> {
    return this.http.post<LoanApplication>(BASE_URL, request);
  }

  /** GET /api/v1/customer/loan-applications — applications belonging to the signed-in customer */
  myApplications(): Observable<LoanApplication[]> {
    return this.http.get<LoanApplication[]>(BASE_URL);
  }

  /** GET /api/v1/customer/loan-applications/{applicationRef} */
  getByRef(applicationRef: string): Observable<LoanApplication> {
    return this.http.get<LoanApplication>(`${BASE_URL}/${applicationRef}`);
  }

  /** POST /api/v1/customer/loan-applications/{applicationRef}/submit — DRAFT -> SUBMITTED */
  submit(applicationRef: string): Observable<LoanApplication> {
    return this.http.post<LoanApplication>(`${BASE_URL}/${applicationRef}/submit`, {});
  }
}