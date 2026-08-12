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
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { CustomerLoanApplicationService } from '../../core/services/customer-loan-application.service';
import { CustomerCreditScoringService } from '../../core/services/customer-credit-scoring.service';
import { CreditScore, LoanApplication } from '../../core/models';

/**
 * Customer view of a single application: summary, a "Submit Application" action (the
 * only lifecycle transition a customer can trigger — DRAFT -> SUBMITTED), and a
 * read-only credit score once one exists. Review, approval-decision, and disbursement
 * are staff/back-office actions performed on the internal LOS console, not here — there
 * is no approval-stage or disbursement-result model on the customer side to support them.
 */
@Component({
  selector: 'los-loan-application-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './loan-application-detail.component.html',
  styleUrl: './loan-application-detail.component.scss',
})
export class LoanApplicationDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly customerLoanApplicationService = inject(CustomerLoanApplicationService);
  private readonly customerCreditScoringService = inject(CustomerCreditScoringService);

  /** Statuses for which a credit score is expected to exist and should be fetched. */
  /** Statuses for which a credit score is expected to exist and should be fetched. */
  private readonly scoredStatuses: readonly LoanApplication['status'][] = [
    'UNDER_REVIEW',
    'APPROVED',
    'DISBURSED',
  ];

  loading = signal(true);
  application = signal<LoanApplication | null>(null);
  loadError = signal<string | null>(null);

  actionInFlight = signal(false);
  actionError = signal<string | null>(null);

  creditScore = signal<CreditScore | null>(null);
  creditScoreError = signal<string | null>(null);

  private get applicationRef(): string {
    return this.route.snapshot.paramMap.get('applicationRef')!;
  }

  ngOnInit(): void {
    this.loadApplication();
  }

  private loadApplication(): void {
    this.loading.set(true);
    this.customerLoanApplicationService.getByRef(this.applicationRef).subscribe({
      next: (app) => {
        this.application.set(app);
        this.loading.set(false);
        if (this.scoredStatuses.includes(app.status)) {
          this.loadCreditScore();
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.loadError.set(err?.message ?? 'Application not found.');
      },
    });
  }

  private loadCreditScore(): void {
    this.customerCreditScoringService.getForApplication(this.applicationRef).subscribe({
      next: (score) => this.creditScore.set(score),
      error: (err) => this.creditScoreError.set(err?.message ?? 'Credit score not available yet.'),
    });
  }

  submitApplication(): void {
    this.actionInFlight.set(true);
    this.actionError.set(null);
    this.customerLoanApplicationService.submit(this.applicationRef).subscribe({
      next: (app) => {
        this.actionInFlight.set(false);
        this.application.set(app);
      },
      error: (err) => {
        this.actionInFlight.set(false);
        this.actionError.set(err?.message ?? 'Could not submit application.');
      },
    });
  }
}
