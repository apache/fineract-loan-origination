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
import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { finalize, take } from 'rxjs';
import { CustomerLoanApplicationService } from '../../core/services/customer-loan-application.service';
import { LoanApplication } from '../../core/models';

@Component({
  selector: 'los-my-loans',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-loans.component.html',
  styleUrl: './my-loans.component.scss',
})
export class MyLoansComponent implements OnInit {
  private readonly customerLoanApplicationService = inject(CustomerLoanApplicationService);
  private readonly cdr = inject(ChangeDetectorRef);

  loading = true;
  error: string | null = null;
  loans: LoanApplication[] = [];

  ngOnInit(): void {
    this.customerLoanApplicationService
      .myApplications()
      .pipe(
        take(1),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (apps) => {
          this.loans = apps.filter((a) => a.status === 'DISBURSED');
        },
        error: (err) => {
          const statusCode = err?.status;
          if (statusCode === 401) {
            this.error = 'Session expired. Please log in again.';
          } else if (statusCode === 0) {
            this.error = 'Cannot connect to server. Please check that the backend is running.';
          } else {
            this.error = err?.error?.message ?? err?.message ?? 'Could not load your loans.';
          }
        },
      });
  }
}
