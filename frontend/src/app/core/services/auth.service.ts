import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, map, switchMap, tap, of, catchError } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Backend uses stateless HTTP Basic Auth (see SecurityConfig — httpBasic(), formLogin disabled,
 * no token endpoint exists). login() confirms credentials by calling a real protected
 * customer-scoped endpoint, then fetches the authenticated customer's real profile from
 * /api/v1/customer/me (backed by CustomerPrincipal on the server) rather than deriving anything
 * client-side.
 *
 * Credentials are held in memory only (never localStorage/sessionStorage) — XSS surface.
 * A page reload requires re-entering credentials, an acceptable trade-off for a stateless
 * Basic Auth POC.
 */
export interface Credentials {
  username: string;
  password: string;
}

/** Mirrors CustomerProfileController.CustomerProfileResponse on the backend. */
export interface CustomerProfile {
  clientId: number;
  displayName: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly credentialsSubject = new BehaviorSubject<Credentials | null>(null);
  private readonly profileSubject = new BehaviorSubject<CustomerProfile | null>(null);

  credentials$ = this.credentialsSubject.asObservable();
  profile$ = this.profileSubject.asObservable();

  /**
   * Verifies credentials against the customer-scoped API, then fetches the real profile
   * (clientId + displayName) from the backend before considering login successful.
   */
  login(username: string, password: string): Observable<boolean> {
    const basicAuthHeader = `Basic ${btoa(`${username}:${password}`)}`;

    return this.http
      .get<CustomerProfile>(`${environment.losApiUrl}/customer/me`, {
        headers: { Authorization: basicAuthHeader },
      })
      .pipe(
        tap((profile) => {
          this.credentialsSubject.next({ username, password });
          this.profileSubject.next(profile);
        }),
        map(() => true),
        catchError(() => of(false)),
      );
  }

  /** Builds the Authorization header value for the currently stored credentials, or null. */
  getAuthHeader(): string | null {
    const credentials = this.credentialsSubject.value;
    return credentials ? `Basic ${btoa(`${credentials.username}:${credentials.password}`)}` : null;
  }

  /** Current customer's real display profile, or null if not signed in. */
  getProfile(): CustomerProfile | null {
    return this.profileSubject.value;
  }

  isAuthenticated(): boolean {
    return this.credentialsSubject.value !== null;
  }

  logout(): void {
    this.credentialsSubject.next(null);
    this.profileSubject.next(null);
  }
}