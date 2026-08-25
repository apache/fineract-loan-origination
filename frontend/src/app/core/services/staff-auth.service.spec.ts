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
import { StaffAuthService } from './staff-auth.service';

const LOGIN_URL = 'http://localhost:8082/api/v1/auth/staff/login';

const LO_RESPONSE = {
  token: 'staff.jwt.token',
  username: 'john',
  losRole: 'ROLE_LOAN_OFFICER',
  displayRole: 'Loan Officer',
  tenantId: 'default',
  userType: 'STAFF',
  expiresInMinutes: 15,
};

const CC_RESPONSE = {
  ...LO_RESPONSE,
  username: 'cc',
  losRole: 'ROLE_CREDIT_COMMITTEE',
  displayRole: 'Credit Committee',
};
const BM_RESPONSE = {
  ...LO_RESPONSE,
  username: 'bm',
  losRole: 'ROLE_BRANCH_MANAGER',
  displayRole: 'Branch Manager',
};

describe('StaffAuthService', () => {
  let service: StaffAuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [StaffAuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(StaffAuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  // ── Initial state ────────────────────────────────────────────────────────

  it('is not authenticated initially', () => {
    expect(service.isAuthenticated()).toBe(false);
  });

  it('returns null token before login', () => {
    expect(service.getToken()).toBeNull();
  });

  it('returns null profile before login', () => {
    expect(service.getProfile()).toBeNull();
  });

  // ── Successful login ─────────────────────────────────────────────────────

  it('returns true on successful login', () => {
    let result: boolean | undefined;
    service.login('john', 'pass').subscribe((v) => (result = v));
    http.expectOne(LOGIN_URL).flush(LO_RESPONSE);
    expect(result).toBe(true);
  });

  it('becomes authenticated after login', () => {
    service.login('john', 'pass').subscribe();
    http.expectOne(LOGIN_URL).flush(LO_RESPONSE);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('stores Bearer token in auth header', () => {
    service.login('john', 'pass').subscribe();
    http.expectOne(LOGIN_URL).flush(LO_RESPONSE);
    expect(service.getAuthHeader()).toBe('Bearer staff.jwt.token');
  });

  it('builds profile with username, losRole, displayRole, tenantId', () => {
    service.login('john', 'pass').subscribe();
    http.expectOne(LOGIN_URL).flush(LO_RESPONSE);
    const p = service.getProfile();
    expect(p?.username).toBe('john');
    expect(p?.losRole).toBe('ROLE_LOAN_OFFICER');
    expect(p?.displayRole).toBe('Loan Officer');
    expect(p?.tenantId).toBe('default');
  });

  it('persists token to sessionStorage', () => {
    service.login('john', 'pass').subscribe();
    http.expectOne(LOGIN_URL).flush(LO_RESPONSE);
    expect(sessionStorage.getItem('los-staff-token')).toBe('staff.jwt.token');
  });

  // ── Role helpers ─────────────────────────────────────────────────────────

  it('isLoanOfficer returns true for LOAN_OFFICER role', () => {
    service.login('john', 'pass').subscribe();
    http.expectOne(LOGIN_URL).flush(LO_RESPONSE);
    expect(service.isLoanOfficer()).toBe(true);
    expect(service.isCreditCommittee()).toBe(false);
    expect(service.isBranchManager()).toBe(false);
  });

  it('isCreditCommittee returns true for CREDIT_COMMITTEE role', () => {
    service.login('cc', 'pass').subscribe();
    http.expectOne(LOGIN_URL).flush(CC_RESPONSE);
    expect(service.isCreditCommittee()).toBe(true);
    expect(service.isLoanOfficer()).toBe(false);
  });

  it('isBranchManager returns true for BRANCH_MANAGER role', () => {
    service.login('bm', 'pass').subscribe();
    http.expectOne(LOGIN_URL).flush(BM_RESPONSE);
    expect(service.isBranchManager()).toBe(true);
    expect(service.isLoanOfficer()).toBe(false);
  });

  it('getLosRole returns the role string', () => {
    service.login('john', 'pass').subscribe();
    http.expectOne(LOGIN_URL).flush(LO_RESPONSE);
    expect(service.getLosRole()).toBe('ROLE_LOAN_OFFICER');
  });

  it('getDisplayRole returns the display string', () => {
    service.login('john', 'pass').subscribe();
    http.expectOne(LOGIN_URL).flush(LO_RESPONSE);
    expect(service.getDisplayRole()).toBe('Loan Officer');
  });

  // ── Failed login ─────────────────────────────────────────────────────────

  it('returns false on 401', () => {
    let result: boolean | undefined;
    service.login('john', 'wrong').subscribe((v) => (result = v));
    http.expectOne(LOGIN_URL).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(result).toBe(false);
  });

  it('stays unauthenticated after failed login', () => {
    service.login('john', 'wrong').subscribe();
    http.expectOne(LOGIN_URL).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(service.isAuthenticated()).toBe(false);
  });

  // ── Logout ───────────────────────────────────────────────────────────────

  it('clears auth state on logout', () => {
    service.login('john', 'pass').subscribe();
    http.expectOne(LOGIN_URL).flush(LO_RESPONSE);
    service.logout();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.getToken()).toBeNull();
    expect(service.getProfile()).toBeNull();
  });

  it('removes token from sessionStorage on logout', () => {
    service.login('john', 'pass').subscribe();
    http.expectOne(LOGIN_URL).flush(LO_RESPONSE);
    service.logout();
    expect(sessionStorage.getItem('los-staff-token')).toBeNull();
  });

  // ── Request shape ────────────────────────────────────────────────────────

  it('POSTs to staff login URL with credentials and tenantId', () => {
    service.login('john', 'pass').subscribe();
    const req = http.expectOne(LOGIN_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toMatchObject({
      username: 'john',
      password: 'pass',
      tenantId: 'default',
    });
    req.flush(LO_RESPONSE);
  });
});
