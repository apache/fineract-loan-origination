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

const BASE_URL = `${environment.losApiUrl}/loan-applications`;

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
