import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CustomerLoanApplicationService } from '../../core/services/customer-loan-application.service';
import { LoanApplication } from '../../core/models';

@Component({
  selector: 'los-loan-application-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './loan-application-list.component.html',
  styleUrl: './loan-application-list.component.scss',
})
export class LoanApplicationListComponent implements OnInit {
  private readonly customerLoanApplicationService = inject(CustomerLoanApplicationService);

  loading = signal(true);
  applications = signal<LoanApplication[]>([]);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.customerLoanApplicationService.myApplications().subscribe({
      next: (apps) => {
        this.applications.set(apps);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message ?? 'Could not load applications.');
        this.loading.set(false);
      },
    });
  }
}
