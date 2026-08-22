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

import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { staffAuthInterceptor } from './staff-auth.interceptor';
import { StaffAuthService } from '../services/staff-auth.service';

const STAFF_URL = 'http://localhost:8082/api/v1/loan-applications';
const AUTH_URL = 'http://localhost:8082/api/v1/auth/login';
const CUSTOMER_URL = 'http://localhost:8082/api/v1/customer/loan-applications';

describe('staffAuthInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let staffAuth: StaffAuthService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        StaffAuthService,
        provideHttpClient(withInterceptors([staffAuthInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    staffAuth = TestBed.inject(StaffAuthService);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('passes through without header when no staff token exists', () => {
    vi.spyOn(staffAuth, 'getAuthHeader').mockReturnValue(null);
    http.get(STAFF_URL).subscribe();
    const req = httpMock.expectOne(STAFF_URL);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('attaches Authorization header to staff API requests', () => {
    vi.spyOn(staffAuth, 'getAuthHeader').mockReturnValue('Bearer staff-token');
    vi.spyOn(staffAuth, 'getTenantId').mockReturnValue('default');
    http.get(STAFF_URL).subscribe();
    const req = httpMock.expectOne(STAFF_URL);
    expect(req.request.headers.get('Authorization')).toBe('Bearer staff-token');
    req.flush([]);
  });

  it('does NOT attach staff token to /api/v1/auth/ endpoints', () => {
    vi.spyOn(staffAuth, 'getAuthHeader').mockReturnValue('Bearer staff-token');
    vi.spyOn(staffAuth, 'getTenantId').mockReturnValue('default');
    http.post(AUTH_URL, {}).subscribe();
    const req = httpMock.expectOne(AUTH_URL);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('does NOT attach staff token to /api/v1/customer/ endpoints', () => {
    vi.spyOn(staffAuth, 'getAuthHeader').mockReturnValue('Bearer staff-token');
    vi.spyOn(staffAuth, 'getTenantId').mockReturnValue('default');
    http.get(CUSTOMER_URL).subscribe();
    const req = httpMock.expectOne(CUSTOMER_URL);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('sets tenant ID header on staff requests', () => {
    vi.spyOn(staffAuth, 'getAuthHeader').mockReturnValue('Bearer staff-token');
    vi.spyOn(staffAuth, 'getTenantId').mockReturnValue('default');
    http.get(STAFF_URL).subscribe();
    const req = httpMock.expectOne(STAFF_URL);
    expect(req.request.headers.get('X-Fineract-Platform-TenantId')).toBe('default');
    req.flush([]);
  });
});
