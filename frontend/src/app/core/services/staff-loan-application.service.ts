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
import {
  ApprovalDecisionRequest,
  StaffApplicationDetail,
  StaffApplicationSummary,
} from '../models/staff-application.model';
import { StaffAuthService } from './staff-auth.service';

@Injectable({ providedIn: 'root' })
export class StaffLoanApplicationService {
  private readonly http = inject(HttpClient);
  private readonly staffAuth = inject(StaffAuthService);

  /** Build auth headers directly — belt-and-suspenders alongside the interceptor. */
  private authHeaders(): Record<string, string> {
    const token = this.staffAuth.getAuthHeader();
    const tenantId = this.staffAuth.getTenantId();
    return {
      ...(token ? { Authorization: token } : {}),
      'X-Fineract-Platform-TenantId': tenantId,
    };
  }

  /** GET /api/v1/loan-applications */
  getAll(): Observable<StaffApplicationSummary[]> {
    return this.http.get<StaffApplicationSummary[]>(`${environment.losApiUrl}/loan-applications`, {
      headers: this.authHeaders(),
    });
  }

  /** GET /api/v1/loan-applications/{ref}/staff-detail */
  getDetail(applicationRef: string): Observable<StaffApplicationDetail> {
    return this.http.get<StaffApplicationDetail>(
      `${environment.losApiUrl}/loan-applications/${applicationRef}/staff-detail`,
      { headers: this.authHeaders() },
    );
  }

  /** POST /api/v1/loan-applications/{ref}/start-review */
  startReview(applicationRef: string): Observable<StaffApplicationSummary> {
    return this.http.post<StaffApplicationSummary>(
      `${environment.losApiUrl}/loan-applications/${applicationRef}/start-review`,
      {},
      { headers: this.authHeaders() },
    );
  }

  /** POST /api/v1/loan-applications/{ref}/approval-decisions */
  recordDecision(applicationRef: string, request: ApprovalDecisionRequest): Observable<unknown> {
    return this.http.post(
      `${environment.losApiUrl}/loan-applications/${applicationRef}/approval-decisions`,
      request,
      { headers: this.authHeaders() },
    );
  }
}
