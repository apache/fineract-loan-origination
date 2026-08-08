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
import { BehaviorSubject, Observable, map, of, catchError, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CustomerProfile {
  clientId: number;
  displayName: string;
  role?: string;
  userType?: string;
}

interface LoginResponse {
  token: string;
  username: string;
  clientId: number;
  tenantId: string;
  role: string;
  userType: string;
  expiresInMinutes: number;
}

const TOKEN_KEY   = 'los-customer-token';
const PROFILE_KEY = 'los-customer-profile';
const TENANT_KEY  = 'los-customer-tenant';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenSubject   = new BehaviorSubject<string | null>(this.loadToken());
  private readonly profileSubject = new BehaviorSubject<CustomerProfile | null>(this.loadProfile());
  private readonly tenantIdSubject = new BehaviorSubject<string>(this.loadTenantId());

  profile$ = this.profileSubject.asObservable();
  readonly tenantId$ = this.tenantIdSubject.asObservable();

  login(username: string, password: string): Observable<boolean> {
    return this.http
      .post<LoginResponse>(`${environment.losApiUrl}/auth/login`, {
        username,
        password,
        tenantId: environment.tenantId,
      })
      .pipe(
        tap((res) => {
          const profile: CustomerProfile = {
            clientId: res.clientId,
            displayName: res.username,
            role: res.role,
            userType: res.userType,
          };
          this.tokenSubject.next(res.token);
          this.profileSubject.next(profile);
          this.setTenantId(res.tenantId);
          try {
            sessionStorage.setItem(TOKEN_KEY,   res.token);
            sessionStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
            sessionStorage.setItem(TENANT_KEY,  res.tenantId);
          } catch { /* storage unavailable */ }
        }),
        map(() => true),
        catchError(() => of(false)),
      );
  }

  getAuthHeader(): string | null {
    const token = this.tokenSubject.value;
    return token ? `Bearer ${token}` : null;
  }

  getTenantId(): string {
    return this.tenantIdSubject.value;
  }

  setTenantId(tenantId: string): void {
    this.tenantIdSubject.next(tenantId?.trim() || 'default');
  }

  getProfile(): CustomerProfile | null {
    return this.profileSubject.value;
  }

  isAuthenticated(): boolean {
    return this.tokenSubject.value !== null;
  }

  logout(): void {
    this.tokenSubject.next(null);
    this.profileSubject.next(null);
    this.tenantIdSubject.next('default');
    try {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(PROFILE_KEY);
      sessionStorage.removeItem(TENANT_KEY);
    } catch { /* ignore */ }
  }

  // ── Session persistence ──────────────────────────────────────────────────

  private loadToken(): string | null {
    try { return sessionStorage.getItem(TOKEN_KEY); } catch { return null; }
  }

  private loadProfile(): CustomerProfile | null {
    try {
      const raw = sessionStorage.getItem(PROFILE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch { return null; }
  }

  private loadTenantId(): string {
    try { return sessionStorage.getItem(TENANT_KEY) ?? environment.tenantId; } catch { return environment.tenantId; }
  }
}