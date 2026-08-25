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
import { AuthService } from './auth.service';

const LOGIN_URL = 'http://localhost:8082/api/v1/auth/login';

const MOCK_RESPONSE = {
  token: 'eyJhbGciOiJIUzI1NiJ9.test',
  username: 'sara',
  clientId: 1,
  tenantId: 'default',
  role: 'ROLE_CUSTOMER',
  userType: 'CUSTOMER',
  expiresInMinutes: 15,
};

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  // ── Initial state ────────────────────────────────────────────────────────

  it('is not authenticated before login', () => {
    expect(service.isAuthenticated()).toBe(false);
  });

  it('returns null auth header before login', () => {
    expect(service.getAuthHeader()).toBeNull();
  });

  it('returns null profile before login', () => {
    expect(service.getProfile()).toBeNull();
  });

  // ── Successful login ─────────────────────────────────────────────────────

  it('returns true on successful login', async () => {
    let result: boolean | undefined;
    service.login('sara', 'sara123').subscribe((v) => (result = v));
    http.expectOne(LOGIN_URL).flush(MOCK_RESPONSE);
    expect(result).toBe(true);
  });

  it('marks authenticated after successful login', () => {
    service.login('sara', 'sara123').subscribe();
    http.expectOne(LOGIN_URL).flush(MOCK_RESPONSE);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('stores Bearer token in auth header after login', () => {
    service.login('sara', 'sara123').subscribe();
    http.expectOne(LOGIN_URL).flush(MOCK_RESPONSE);
    expect(service.getAuthHeader()).toBe(`Bearer ${MOCK_RESPONSE.token}`);
  });

  it('builds profile from login response', () => {
    service.login('sara', 'sara123').subscribe();
    http.expectOne(LOGIN_URL).flush(MOCK_RESPONSE);
    const profile = service.getProfile();
    expect(profile?.clientId).toBe(1);
    expect(profile?.displayName).toBe('sara');
  });

  it('sets tenant ID from login response', () => {
    service.login('sara', 'sara123').subscribe();
    http.expectOne(LOGIN_URL).flush(MOCK_RESPONSE);
    expect(service.getTenantId()).toBe('default');
  });

  it('persists token in sessionStorage after login', () => {
    service.login('sara', 'sara123').subscribe();
    http.expectOne(LOGIN_URL).flush(MOCK_RESPONSE);
    expect(sessionStorage.getItem('los-customer-token')).toBe(MOCK_RESPONSE.token);
  });

  // ── Failed login ─────────────────────────────────────────────────────────

  it('returns false on 401 response', async () => {
    let result: boolean | undefined;
    service.login('sara', 'wrong').subscribe((v) => (result = v));
    http
      .expectOne(LOGIN_URL)
      .flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
    expect(result).toBe(false);
  });

  it('remains unauthenticated after failed login', () => {
    service.login('sara', 'wrong').subscribe();
    http.expectOne(LOGIN_URL).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(service.isAuthenticated()).toBe(false);
  });

  it('keeps null profile after failed login', () => {
    service.login('sara', 'wrong').subscribe();
    http.expectOne(LOGIN_URL).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(service.getProfile()).toBeNull();
  });

  // ── Logout ───────────────────────────────────────────────────────────────

  it('clears auth state on logout', () => {
    service.login('sara', 'sara123').subscribe();
    http.expectOne(LOGIN_URL).flush(MOCK_RESPONSE);

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.getAuthHeader()).toBeNull();
    expect(service.getProfile()).toBeNull();
  });

  it('clears sessionStorage on logout', () => {
    service.login('sara', 'sara123').subscribe();
    http.expectOne(LOGIN_URL).flush(MOCK_RESPONSE);

    service.logout();

    expect(sessionStorage.getItem('los-customer-token')).toBeNull();
  });

  it('resets tenant to default on logout', () => {
    service.login('sara', 'sara123').subscribe();
    http.expectOne(LOGIN_URL).flush(MOCK_RESPONSE);

    service.logout();

    expect(service.getTenantId()).toBe('default');
  });

  // ── Persistence ──────────────────────────────────────────────────────────

  it('sends login request to correct URL with credentials', () => {
    service.login('sara', 'sara123').subscribe();
    const req = http.expectOne(LOGIN_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toMatchObject({ username: 'sara', password: 'sara123' });
    req.flush(MOCK_RESPONSE);
  });

  it('includes tenantId in login request body', () => {
    service.login('sara', 'sara123').subscribe();
    const req = http.expectOne(LOGIN_URL);
    expect(req.request.body.tenantId).toBe('default');
    req.flush(MOCK_RESPONSE);
  });
});
