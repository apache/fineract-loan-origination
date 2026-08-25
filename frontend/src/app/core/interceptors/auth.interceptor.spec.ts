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
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

const TEST_URL = 'http://localhost:8082/api/v1/customer/some-endpoint';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('does not add Authorization header when unauthenticated', () => {
    vi.spyOn(authService, 'getAuthHeader').mockReturnValue(null);
    http.get(TEST_URL).subscribe();
    const req = httpMock.expectOne(TEST_URL);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('adds Authorization header when authenticated', () => {
    vi.spyOn(authService, 'getAuthHeader').mockReturnValue('Bearer test-token');
    vi.spyOn(authService, 'getTenantId').mockReturnValue('default');
    http.get(TEST_URL).subscribe();
    const req = httpMock.expectOne(TEST_URL);
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-token');
    req.flush({});
  });

  it('adds X-Fineract-Platform-TenantId header when authenticated', () => {
    vi.spyOn(authService, 'getAuthHeader').mockReturnValue('Bearer test-token');
    vi.spyOn(authService, 'getTenantId').mockReturnValue('default');
    http.get(TEST_URL).subscribe();
    const req = httpMock.expectOne(TEST_URL);
    expect(req.request.headers.get('X-Fineract-Platform-TenantId')).toBe('default');
    req.flush({});
  });

  it('skips if Authorization header already set', () => {
    vi.spyOn(authService, 'getAuthHeader').mockReturnValue('Bearer test-token');
    vi.spyOn(authService, 'getTenantId').mockReturnValue('default');
    http.get(TEST_URL, { headers: { Authorization: 'Bearer already-set' } }).subscribe();
    const req = httpMock.expectOne(TEST_URL);
    // Should keep the already-set value, not overwrite it
    expect(req.request.headers.get('Authorization')).toBe('Bearer already-set');
    req.flush({});
  });
});
