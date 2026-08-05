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
import { CustomerLoanApplicationService } from '../../core/services/customer-loan-application.service';
import { CreateLoanApplicationRequest } from '../../core/models';

@Component({
  selector: 'los-loan-application-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './loan-application-create.component.html',
  styleUrl: './loan-application-create.component.scss',
})
export class LoanApplicationCreateComponent {
  private readonly fb = inject(FormBuilder);
  private readonly customerLoanApplicationService = inject(CustomerLoanApplicationService);
  private readonly router = inject(Router);

  submitting = signal(false);
  error = signal<string | null>(null);

  form = this.fb.group({
    requestedAmount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    currency: ['USD'],
    loanPurpose: ['BUSINESS'],
    tenorMonths: [12, [Validators.required, Validators.min(1)]],
    fineractLoanProductId: [1 as number | null],
    applicant: this.fb.group({
      fullName: ['', Validators.required],
      nationalId: [''],
      monthlyIncome: [null as number | null, Validators.min(0)],
      employmentStatus: ['EMPLOYED'],
      employmentDurationMonths: [null as number | null, Validators.min(0)],
      existingLoanObligations: [0, Validators.min(0)],
      fineractClientId: [null as number | null],
    }),
  });

  submit(): void {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.error.set(null);

    const request = this.form.getRawValue() as unknown as CreateLoanApplicationRequest;

    this.customerLoanApplicationService.create(request).subscribe({
      next: (application) => {
        this.submitting.set(false);
        this.router.navigate(['/loan-application', application.applicationRef]);
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err?.message ?? 'Could not create application — please try again.');
      },
    });
  }
}
