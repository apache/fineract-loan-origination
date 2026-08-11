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
import { Observable, catchError, throwError, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateLoanApplicationRequest, LoanApplication } from '../models';
import { AuthService } from './auth.service';

const BASE_URL = `${environment.losApiUrl}/customer/loan-applications`;
const REQUEST_TIMEOUT_MS = 15000;

@Injectable({ providedIn: 'root' })
export class CustomerLoanApplicationService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  private authHeaders(): Record<string, string> {
    const token    = this.auth.getAuthHeader();
    const tenantId = this.auth.getTenantId();
    return {
      ...(token ? { Authorization: token } : {}),
      'X-Fineract-Platform-TenantId': tenantId,
    };
  }

  /** POST /api/v1/customer/loan-applications */
  create(request: CreateLoanApplicationRequest): Observable<LoanApplication> {
    return this.http.post<LoanApplication>(BASE_URL, request, { headers: this.authHeaders() }).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => throwError(() => new Error('Could not create your application.'))),
    );
  }

  /** GET /api/v1/customer/loan-applications */
  myApplications(): Observable<LoanApplication[]> {
    return this.http.get<LoanApplication[]>(BASE_URL, { headers: this.authHeaders() }).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => throwError(() => new Error('Could not load your applications.'))),
    );
  }

  /** GET /api/v1/customer/loan-applications/{applicationRef} */
  getByRef(applicationRef: string): Observable<LoanApplication> {
    return this.http
      .get<LoanApplication>(`${BASE_URL}/${applicationRef}`, { headers: this.authHeaders() })
      .pipe(
        timeout(REQUEST_TIMEOUT_MS),
        catchError(() => throwError(() => new Error('Could not load this application.'))),
      );
  }

  /** POST /api/v1/customer/loan-applications/{applicationRef}/submit */
  submit(applicationRef: string): Observable<LoanApplication> {
    return this.http
      .post<LoanApplication>(
        `${BASE_URL}/${applicationRef}/submit`,
        {},
        { headers: this.authHeaders() },
      )
      .pipe(
        timeout(REQUEST_TIMEOUT_MS),
        catchError(() => throwError(() => new Error('Could not submit your application.'))),
      );
  }
}
