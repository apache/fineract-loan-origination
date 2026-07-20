import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
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

  loading = true;
  error: string | null = null;
  loans: LoanApplication[] = [];

  ngOnInit(): void {
    this.customerLoanApplicationService.myApplications().subscribe({
      next: (apps) => {
        this.loans = apps.filter((a) => a.status === 'DISBURSED');
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.message ?? 'Could not load your loans.';
        this.loading = false;
      },
    });
  }
}