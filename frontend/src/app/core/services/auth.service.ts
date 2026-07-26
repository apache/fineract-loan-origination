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
}

interface LoginResponse {
  token: string;
  username: string;
  clientId: number;
  tenantId: string;
  expiresInMinutes: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenSubject = new BehaviorSubject<string | null>(null);
  private readonly profileSubject = new BehaviorSubject<CustomerProfile | null>(null);

  profile$ = this.profileSubject.asObservable();

  login(username: string, password: string): Observable<boolean> {
    return this.http
      .post<LoginResponse>(`${environment.losApiUrl}/auth/login`, {
        username,
        password,
        tenantId: environment.tenantId,
      })
      .pipe(
        tap((res) => {
          this.tokenSubject.next(res.token);
          this.profileSubject.next({ clientId: res.clientId, displayName: res.username });
        }),
        map(() => true),
        catchError(() => of(false)),
      );
  }

  getAuthHeader(): string | null {
    const token = this.tokenSubject.value;
    return token ? `Bearer ${token}` : null;
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
  }
}
