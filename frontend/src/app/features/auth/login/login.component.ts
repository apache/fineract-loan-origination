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
    <form [formGroup]="form" (ngSubmit)="submit()" class="login-form">
      <h2>Loan Origination — Sign In</h2>

      <label>
        Username
        <input
          formControlName="username"
          autocomplete="username"
        />
      </label>

      <label>
        Password
        <input
          type="password"
          formControlName="password"
          autocomplete="current-password"
        />
      </label>

      @if (error()) {
        <p class="error">{{ error() }}</p>
      }

      <button
        type="submit"
        [disabled]="form.invalid || submitting()"
      >
        {{ submitting() ? 'Signing in…' : 'Sign in' }}
      </button>
    </form>
  `,
})
export class LoginComponent {
  // ---------------------------------------------------------------------------
  // Dependencies
  // ---------------------------------------------------------------------------

  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // ---------------------------------------------------------------------------
  // UI State
  // ---------------------------------------------------------------------------

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  // ---------------------------------------------------------------------------
  // Form
  // ---------------------------------------------------------------------------

  readonly form = this.fb.group({
    username: this.fb.nonNullable.control('', Validators.required),
    password: this.fb.nonNullable.control('', Validators.required),
  });

  // ---------------------------------------------------------------------------
  // Actions
  // ---------------------------------------------------------------------------

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
          this.router.navigate(['/loan-application']);
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