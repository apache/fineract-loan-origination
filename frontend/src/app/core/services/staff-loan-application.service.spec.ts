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

import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { StaffLoanApplicationService } from './staff-loan-application.service';
import { StaffAuthService } from './staff-auth.service';

const BASE = 'http://localhost:8082/api/v1/loan-applications';

const SUMMARY = {
  applicationRef: 'LOS-2026-00001',
  status: 'UNDER_REVIEW',
  requestedAmount: 5000,
  currency: 'USD',
  loanPurpose: 'BUSINESS',
  tenorMonths: 12,
  createdAt: '2026-08-01T00:00:00',
  updatedAt: '2026-08-01T00:00:00',
  fineractLoanId: null,
  applicantName: 'Sara Johnson',
  fineractClientId: 1,
};

const DETAIL = {
  ...SUMMARY,
  applicantName: 'Sara Johnson',
  nationalId: '123456',
  monthlyIncome: 5000,
  employmentStatus: 'EMPLOYED',
  creditScore: {
    score: 76,
    riskRating: 'LOW',
    incomeRatioScore: 15,
    debtBurdenScore: 25,
    employmentScore: 18,
    repaymentHistoryScore: 8,
    loanPurposeScore: 10,
    scoredAt: '2026-08-01T00:00:00',
  },
  approvalStages: [],
  currentApprovalStage: 'LOAN_OFFICER',
  fineractIntegrationStatus: null,
  disbursedAt: null,
};

describe('StaffLoanApplicationService', () => {
  let service: StaffLoanApplicationService;
  let http: HttpTestingController;
  let staffAuth: StaffAuthService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        StaffLoanApplicationService,
        StaffAuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(StaffLoanApplicationService);
    http = TestBed.inject(HttpTestingController);
    staffAuth = TestBed.inject(StaffAuthService);

    vi.spyOn(staffAuth, 'getAuthHeader').mockReturnValue('Bearer staff-token');
    vi.spyOn(staffAuth, 'getTenantId').mockReturnValue('default');
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  // ── getAll ───────────────────────────────────────────────────────────────

  it('GET /loan-applications returns list', () => {
    let result: unknown;
    service.getAll().subscribe((v) => (result = v));
    const req = http.expectOne(BASE);
    expect(req.request.method).toBe('GET');
    req.flush([SUMMARY]);
    expect(result).toEqual([SUMMARY]);
  });

  it('attaches Authorization header on getAll', () => {
    service.getAll().subscribe();
    const req = http.expectOne(BASE);
    expect(req.request.headers.get('Authorization')).toBe('Bearer staff-token');
    req.flush([]);
  });

  // ── getDetail ────────────────────────────────────────────────────────────

  it('GET /loan-applications/{ref}/staff-detail returns detail', () => {
    let result: unknown;
    service.getDetail('LOS-2026-00001').subscribe((v) => (result = v));
    const req = http.expectOne(`${BASE}/LOS-2026-00001/staff-detail`);
    expect(req.request.method).toBe('GET');
    req.flush(DETAIL);
    expect((result as typeof DETAIL).applicationRef).toBe('LOS-2026-00001');
  });

  // ── startReview ──────────────────────────────────────────────────────────

  it('POST /loan-applications/{ref}/start-review with empty body', () => {
    service.startReview('LOS-2026-00001').subscribe();
    const req = http.expectOne(`${BASE}/LOS-2026-00001/start-review`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(SUMMARY);
  });

  // ── recordDecision ───────────────────────────────────────────────────────

  it('POST /approval-decisions with APPROVE decision', () => {
    const decision = { decision: 'APPROVE' as const, comments: 'Good credit score' };
    service.recordDecision('LOS-2026-00001', decision).subscribe();
    const req = http.expectOne(`${BASE}/LOS-2026-00001/approval-decisions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(decision);
    req.flush({});
  });

  it('POST /approval-decisions with REFER decision', () => {
    const decision = { decision: 'REFER' as const, comments: 'Need more documents please provide' };
    service.recordDecision('LOS-2026-00001', decision).subscribe();
    const req = http.expectOne(`${BASE}/LOS-2026-00001/approval-decisions`);
    expect(req.request.body.decision).toBe('REFER');
    req.flush({});
  });

  it('POST /approval-decisions with REJECT decision', () => {
    const decision = { decision: 'REJECT' as const, comments: 'Insufficient income documentation' };
    service.recordDecision('LOS-2026-00001', decision).subscribe();
    const req = http.expectOne(`${BASE}/LOS-2026-00001/approval-decisions`);
    expect(req.request.body.decision).toBe('REJECT');
    req.flush({});
  });

  // ── disburse ─────────────────────────────────────────────────────────────

  it('POST /loan-applications/{ref}/disburse returns loanId', () => {
    let result: { loanId: number } | undefined;
    service.disburse('LOS-2026-00001').subscribe((v) => (result = v));
    const req = http.expectOne(`${BASE}/LOS-2026-00001/disburse`);
    expect(req.request.method).toBe('POST');
    req.flush({ loanId: 100001 });
    expect(result?.loanId).toBe(100001);
  });
});
