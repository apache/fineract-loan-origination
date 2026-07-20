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
        if (app.status === 'UNDER_REVIEW' || app.status === 'APPROVED' || app.status === 'DISBURSED') {
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
