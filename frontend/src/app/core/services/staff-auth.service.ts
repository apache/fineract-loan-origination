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
import { BehaviorSubject, Observable, catchError, map, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface StaffProfile {
  username: string;
  losRole: string; // e.g. ROLE_LOAN_OFFICER
  displayRole: string; // e.g. "Loan Officer"
  tenantId: string;
}

interface StaffLoginResponse {
  token: string;
  username: string;
  losRole: string;
  displayRole: string;
  tenantId: string;
  userType: string;
  expiresInMinutes: number;
}

const TOKEN_KEY = 'los-staff-token';
const PROFILE_KEY = 'los-staff-profile';

@Injectable({ providedIn: 'root' })
export class StaffAuthService {
  private readonly http = inject(HttpClient);

  private readonly tokenSubject = new BehaviorSubject<string | null>(this.loadToken());
  private readonly profileSubject = new BehaviorSubject<StaffProfile | null>(this.loadProfile());

  readonly profile$ = this.profileSubject.asObservable();

  // -------------------------------------------------------------------------
  // Auth
  // -------------------------------------------------------------------------

  login(username: string, password: string): Observable<boolean> {
    return this.http
      .post<StaffLoginResponse>(`${environment.losApiUrl}/auth/staff/login`, {
        username,
        password,
        tenantId: environment.tenantId,
      })
      .pipe(
        tap((res) => {
          const profile: StaffProfile = {
            username: res.username,
            losRole: res.losRole,
            displayRole: res.displayRole,
            tenantId: res.tenantId,
          };
          this.tokenSubject.next(res.token);
          this.profileSubject.next(profile);
          try {
            sessionStorage.setItem(TOKEN_KEY, res.token);
            sessionStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
          } catch {
            /* storage unavailable */
          }
        }),
        map(() => true),
        catchError(() => of(false)),
      );
  }

  logout(): void {
    this.tokenSubject.next(null);
    this.profileSubject.next(null);
    try {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(PROFILE_KEY);
    } catch {
      /* ignore */
    }
  }

  // -------------------------------------------------------------------------
  // Accessors
  // -------------------------------------------------------------------------

  isAuthenticated(): boolean {
    return !!this.tokenSubject.value;
  }

  getToken(): string | null {
    return this.tokenSubject.value;
  }

  getAuthHeader(): string | null {
    const t = this.tokenSubject.value;
    return t ? `Bearer ${t}` : null;
  }

  getProfile(): StaffProfile | null {
    return this.profileSubject.value;
  }

  getTenantId(): string {
    return this.profileSubject.value?.tenantId ?? environment.tenantId;
  }

  /** e.g. "ROLE_LOAN_OFFICER" */
  getLosRole(): string | null {
    return this.profileSubject.value?.losRole ?? null;
  }

  /** e.g. "Loan Officer" */
  getDisplayRole(): string | null {
    return this.profileSubject.value?.displayRole ?? null;
  }

  hasRole(role: string): boolean {
    return this.profileSubject.value?.losRole === role;
  }

  isLoanOfficer(): boolean {
    return this.hasRole('ROLE_LOAN_OFFICER');
  }
  isCreditCommittee(): boolean {
    return this.hasRole('ROLE_CREDIT_COMMITTEE');
  }
  isBranchManager(): boolean {
    return this.hasRole('ROLE_BRANCH_MANAGER');
  }

  // -------------------------------------------------------------------------
  // Session persistence
  // -------------------------------------------------------------------------

  private loadToken(): string | null {
    try {
      return sessionStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  }

  private loadProfile(): StaffProfile | null {
    try {
      const raw = sessionStorage.getItem(PROFILE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
