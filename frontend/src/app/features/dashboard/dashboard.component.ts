import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CustomerLoanApplicationService } from '../../core/services/customer-loan-application.service';
import { AuthService } from '../../core/services/auth.service';
import { LoanApplication } from '../../core/models';

@Component({
  selector: 'los-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly customerLoanApplicationService = inject(CustomerLoanApplicationService);
  private readonly authService = inject(AuthService);

  loading = true;
  error: string | null = null;
  applications: LoanApplication[] = [];

  get displayName(): string {
    return this.authService.getProfile()?.displayName ?? 'there';
  }

  get activeCount(): number {
    return this.applications.filter(
      (a) => a.status !== 'DISBURSED' && a.status !== 'REJECTED',
    ).length;
  }

  get disbursedCount(): number {
    return this.applications.filter((a) => a.status === 'DISBURSED').length;
  }

  ngOnInit(): void {
    this.customerLoanApplicationService.myApplications().subscribe({
      next: (apps) => {
        this.applications = apps;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.message ?? 'Could not load your applications.';
        this.loading = false;
      },
    });
  }
}