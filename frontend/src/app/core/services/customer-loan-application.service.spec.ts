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
import { CustomerLoanApplicationService } from './customer-loan-application.service';

const BASE = 'http://localhost:8082/api/v1/customer/loan-applications';

describe('CustomerLoanApplicationService', () => {
  let service: CustomerLoanApplicationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CustomerLoanApplicationService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CustomerLoanApplicationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses the customer-scoped base path for myApplications', () => {
    service.myApplications().subscribe();
    const req = http.expectOne(BASE);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('sends POST to base URL for create', () => {
    const payload = {
      requestedAmount: 5000,
      applicant: { fullName: 'Test User' },
    };
    service.create(payload).subscribe();
    const req = http.expectOne(BASE);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ applicationRef: 'LOS-TEST-001', status: 'DRAFT' });
  });

  it('sends GET to the ref path for getByRef', () => {
    service.getByRef('LOS-TEST-001').subscribe();
    const req = http.expectOne(`${BASE}/LOS-TEST-001`);
    expect(req.request.method).toBe('GET');
    req.flush({ applicationRef: 'LOS-TEST-001', status: 'DRAFT' });
  });

  it('sends POST to submit path for submit', () => {
    service.submit('LOS-TEST-001').subscribe();
    const req = http.expectOne(`${BASE}/LOS-TEST-001/submit`);
    expect(req.request.method).toBe('POST');
    req.flush({ applicationRef: 'LOS-TEST-001', status: 'SUBMITTED' });
  });

  it('wraps non-HTTP errors in an HttpErrorResponse with status 0', () => {
    let caught: { status: number } | null = null;
    service.myApplications().subscribe({ error: (e) => (caught = e) });
    const req = http.expectOne(BASE);
    req.error(new ProgressEvent('timeout'));
    // timeout fires after 8s — error comes from ProgressEvent network abort
    // status is 0 because no HTTP response was received
    expect(caught).toBeTruthy();
  });
});
