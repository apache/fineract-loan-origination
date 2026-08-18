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
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateLoanApplicationRequest, LoanApplication } from '../models';
import { AuthService } from './auth.service';

const STAFF_BASE_URL = `${environment.losApiUrl}/loan-applications`;
const CUSTOMER_BASE_URL = `${environment.losApiUrl}/customer/loan-applications`;

@Injectable({ providedIn: 'root' })
export class LoanApplicationService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  private authHeaders(): Record<string, string> {
    const token = this.auth.getAuthHeader();
    const tenantId = this.auth.getTenantId();
    return {
      ...(token ? { Authorization: token } : {}),
      'X-Fineract-Platform-TenantId': tenantId,
    };
  }

  /** POST /api/v1/loan-applications — staff/admin creation path */
  create(request: CreateLoanApplicationRequest): Observable<LoanApplication> {
    return this.http.post<LoanApplication>(STAFF_BASE_URL, request, {
      headers: this.authHeaders(),
    });
  }

  /** GET /api/v1/customer/loan-applications — scoped to the logged-in customer only */
  list(): Observable<LoanApplication[]> {
    return this.http.get<LoanApplication[]>(CUSTOMER_BASE_URL, { headers: this.authHeaders() });
  }

  /** GET /api/v1/loan-applications/{applicationRef} — staff detail lookup */
  getByRef(applicationRef: string): Observable<LoanApplication> {
    return this.http.get<LoanApplication>(`${STAFF_BASE_URL}/${applicationRef}`, {
      headers: this.authHeaders(),
    });
  }

  /** POST /api/v1/customer/loan-applications/{applicationRef}/submit — DRAFT -> SUBMITTED */
  submit(applicationRef: string): Observable<LoanApplication> {
    return this.http.post<LoanApplication>(
      `${CUSTOMER_BASE_URL}/${applicationRef}/submit`,
      {},
      { headers: this.authHeaders() },
    );
  }

  /** POST /api/v1/loan-applications/{applicationRef}/start-review — SUBMITTED -> UNDER_REVIEW */
  startReview(applicationRef: string): Observable<LoanApplication> {
    return this.http.post<LoanApplication>(
      `${STAFF_BASE_URL}/${applicationRef}/start-review`,
      {},
      { headers: this.authHeaders() },
    );
  }
}
