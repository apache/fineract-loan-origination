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

describe('CustomerLoanApplicationService', () => {
  let service: CustomerLoanApplicationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CustomerLoanApplicationService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(CustomerLoanApplicationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('uses the customer-scoped base path for application APIs', () => {
    service.myApplications().subscribe();
    service.myApplications().subscribe();

    const request = httpMock.expectOne('http://localhost:8082/api/v1/customer/loan-applications');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
});
