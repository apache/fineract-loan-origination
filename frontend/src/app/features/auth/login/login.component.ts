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
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'los-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="login-page">
      <div class="login-card">
        <div class="login-header">
          <img src="fineract_logo.png" alt="Apache Fineract" class="logo" />
          <h1>Loan Origination</h1>
          <p>Sign in to your account</p>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label for="username">Username</label>
            <input
              id="username"
              formControlName="username"
              autocomplete="username"
              placeholder="Enter your username"
            />
          </div>

          <div class="field">
            <label for="password">Password</label>
            <input
              id="password"
              type="password"
              formControlName="password"
              autocomplete="current-password"
              placeholder="Enter your password"
            />
          </div>

          @if (error()) {
            <div class="error-banner">{{ error() }}</div>
          }

          <button type="submit" class="submit-btn" [disabled]="form.invalid || submitting()">
            {{ submitting() ? 'Signing in…' : 'Sign in' }}
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [
    `
      .login-page {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--color-bg);
        padding: 1rem;
      }

      .login-card {
        width: 100%;
        max-width: 400px;
        background: var(--color-surface);
        border: 1px solid var(--color-border);
        border-radius: 12px;
        padding: 2.5rem 2rem;
        box-shadow: 0 4px 24px rgba(0, 0, 0, 0.07);
      }

      .login-header {
        text-align: center;
        margin-bottom: 2rem;
      }

      .logo {
        height: 48px;
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
        font-size: 0.9rem;
        margin: 0;
      }

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

        &:focus {
          border-color: var(--color-primary);
        }

        &::placeholder {
          color: var(--color-text-faint);
        }
      }

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

        &:hover:not(:disabled) {
          background: var(--color-primary-dark);
        }

        &:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
      }
    `,
  ],
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    username: this.fb.nonNullable.control('', Validators.required),
    password: this.fb.nonNullable.control('', Validators.required),
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.error.set(null);

    const { username, password } = this.form.getRawValue();

    this.authService.login(username, password).subscribe({
      next: (success) => {
        this.submitting.set(false);
        if (success) {
          this.router.navigate(['/dashboard']);
        } else {
          this.error.set('Invalid username or password.');
        }
      },
      error: () => {
        this.submitting.set(false);
        this.error.set('Invalid username or password.');
      },
    });
  }
}
