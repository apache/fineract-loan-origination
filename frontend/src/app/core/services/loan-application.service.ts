import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateLoanApplicationRequest, LoanApplication } from '../models';

const STAFF_BASE_URL = `${environment.losApiUrl}/loan-applications`;
const CUSTOMER_BASE_URL = `${environment.losApiUrl}/customer/loan-applications`;

@Injectable({ providedIn: 'root' })
export class LoanApplicationService {
  private readonly http = inject(HttpClient);

  /** POST /api/v1/loan-applications — staff/admin creation path */
  create(request: CreateLoanApplicationRequest): Observable<LoanApplication> {
    return this.http.post<LoanApplication>(STAFF_BASE_URL, request);
  }

  /** GET /api/v1/customer/loan-applications — scoped to the logged-in customer only */
  list(): Observable<LoanApplication[]> {
    return this.http.get<LoanApplication[]>(CUSTOMER_BASE_URL);
  }

  /** GET /api/v1/loan-applications/{applicationRef} — staff detail lookup */
  getByRef(applicationRef: string): Observable<LoanApplication> {
    return this.http.get<LoanApplication>(`${STAFF_BASE_URL}/${applicationRef}`);
  }

  /** POST /api/v1/loan-applications/{applicationRef}/submit — DRAFT -> SUBMITTED */
  submit(applicationRef: string): Observable<LoanApplication> {
    return this.http.post<LoanApplication>(`${STAFF_BASE_URL}/${applicationRef}/submit`, {});
  }

  /** POST /api/v1/loan-applications/{applicationRef}/start-review — SUBMITTED -> UNDER_REVIEW */
  startReview(applicationRef: string): Observable<LoanApplication> {
    return this.http.post<LoanApplication>(`${STAFF_BASE_URL}/${applicationRef}/start-review`, {});
  }
}