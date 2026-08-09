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
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { StaffAuthService } from '../../../core/services/staff-auth.service';

@Component({
  selector: 'los-staff-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="staff-login-page">
      <div class="staff-login-card">
        <div class="login-header">
          <img src="fineract_logo.png" alt="Apache Fineract" class="logo" />
          <h1>Staff Portal</h1>
          <p>Sign in with your Fineract credentials</p>
          <div class="roles-hint">
            <span class="role-badge loan-officer">Loan Officer</span>
            <span class="role-badge credit-committee">Credit Committee</span>
            <span class="role-badge branch-manager">Branch Manager</span>
          </div>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label for="username">Username</label>
            <input
              id="username"
              formControlName="username"
              autocomplete="username"
              placeholder="Fineract username"
            />
          </div>

          <div class="field">
            <label for="password">Password</label>
            <input
              id="password"
              type="password"
              formControlName="password"
              autocomplete="current-password"
              placeholder="Fineract password"
            />
          </div>

          @if (error()) {
            <div class="error-banner" role="alert">{{ error() }}</div>
          }

          <button type="submit" class="submit-btn" [disabled]="form.invalid || submitting()">
            {{ submitting() ? 'Signing in…' : 'Sign in' }}
          </button>
        </form>

        <div class="login-footer">
          <a routerLink="/login" href="/login">Customer portal →</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .staff-login-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--color-bg);
      padding: 1rem;
    }

    .staff-login-card {
      width: 100%;
      max-width: 420px;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: 12px;
      padding: 2.5rem 2rem;
      box-shadow: 0 4px 24px rgba(0,0,0,0.08);
    }

    .login-header {
      text-align: center;
      margin-bottom: 2rem;
    }

    .logo {
      height: 44px;
      width: auto;
      margin-bottom: 1rem;
      object-fit: contain;
    }

    h1 {
      font-size: 1.4rem;
      font-weight: 700;
      color: var(--color-text);
      margin: 0 0 0.25rem;
    }

    p {
      color: var(--color-text-muted);
      font-size: 0.875rem;
      margin: 0 0 1rem;
    }

    .roles-hint {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
      justify-content: center;
      margin-top: 0.75rem;
    }

    .role-badge {
      font-size: 0.7rem;
      font-weight: 600;
      padding: 0.2rem 0.6rem;
      border-radius: 99px;
      letter-spacing: 0.02em;
    }

    .loan-officer     { background: #dbeafe; color: #1d4ed8; }
    .credit-committee { background: #fef9c3; color: #a16207; }
    .branch-manager   { background: #dcfce7; color: #15803d; }

    .field {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
      margin-bottom: 1.1rem;
    }

    label {
      font-size: 0.85rem;
      font-weight: 500;
      color: var(--color-text);
    }

    input {
      padding: 0.6rem 0.85rem;
      border: 1px solid var(--color-border);
      border-radius: var(--radius);
      background: var(--color-bg);
      color: var(--color-text);
      font-size: 0.95rem;
      outline: none;
      transition: border-color 0.15s;
    }

    input:focus { border-color: var(--color-primary); }
    input::placeholder { color: var(--color-text-faint); }

    .error-banner {
      background: var(--color-error-bg);
      color: var(--color-error);
      border-radius: var(--radius);
      padding: 0.6rem 0.85rem;
      font-size: 0.875rem;
      margin-bottom: 1rem;
    }

    .submit-btn {
      width: 100%;
      padding: 0.7rem;
      background: var(--color-primary);
      color: var(--color-primary-contrast);
      border: none;
      border-radius: var(--radius);
      font-size: 0.95rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.15s;
    }

    .submit-btn:hover:not(:disabled) { background: var(--color-primary-dark); }
    .submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }

    .login-footer {
      text-align: center;
      margin-top: 1.5rem;
      font-size: 0.85rem;
    }

    .login-footer a {
      color: var(--color-text-muted);
      text-decoration: none;
    }

    .login-footer a:hover { color: var(--color-primary); }
  `],
})
export class StaffLoginComponent {
  private readonly fb          = inject(FormBuilder);
  private readonly staffAuth   = inject(StaffAuthService);
  private readonly router      = inject(Router);

  readonly submitting = signal(false);
  readonly error      = signal<string | null>(null);

  readonly form = this.fb.group({
    username: this.fb.nonNullable.control('', Validators.required),
    password: this.fb.nonNullable.control('', Validators.required),
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    this.submitting.set(true);
    this.error.set(null);

    const { username, password } = this.form.getRawValue();

    this.staffAuth.login(username, password).subscribe({
      next: (ok) => {
        this.submitting.set(false);
        if (ok) {
          this.router.navigate(['/staff/dashboard']);
        } else {
          this.error.set('Invalid credentials or no LOS role assigned to your account.');
        }
      },
      error: () => {
        this.submitting.set(false);
        this.error.set('Login failed. Please check your credentials.');
      },
    });
  }
}
