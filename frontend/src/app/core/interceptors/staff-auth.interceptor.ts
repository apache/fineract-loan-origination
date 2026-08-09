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
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { StaffAuthService } from '../services/staff-auth.service';

/**
 * Attaches the staff JWT Bearer token to requests that are headed for the
 * staff API paths (/api/v1/loan-applications, /api/v1/staff, /api/v1/admin).
 *
 * Deliberately skips /api/v1/auth/* so login endpoints are never polluted
 * with a stale token. Also skips when no staff token exists so customer
 * requests are handled by the existing authInterceptor unchanged.
 */
export const staffAuthInterceptor: HttpInterceptorFn = (req, next) => {
  const staffAuth = inject(StaffAuthService);
  const authHeader = staffAuth.getAuthHeader();

  // No staff token — pass through untouched
  if (!authHeader) return next(req);

  // Never attach to auth endpoints (customer OR staff login)
  if (req.url.includes('/api/v1/auth/')) return next(req);

  // Only attach to requests that come from staff pages
  // (the Angular Router URL starts with /staff/)
  // We detect this by checking whether the page origin URL has /staff/ —
  // but since this is an HTTP interceptor we check the request URL path instead.
  // Staff API calls always go to /api/v1/loan-applications, /api/v1/staff, /api/v1/admin.
  // Customer API calls go to /api/v1/customer/.
  if (req.url.includes('/api/v1/customer/')) return next(req);

  return next(
    req.clone({
      setHeaders: {
        Authorization: authHeader,
        'X-Fineract-Platform-TenantId': staffAuth.getTenantId(),
      },
    }),
  );
};
