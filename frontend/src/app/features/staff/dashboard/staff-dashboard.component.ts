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
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { StaffAuthService } from '../../../core/services/staff-auth.service';
import { StaffLoanApplicationService } from '../../../core/services/staff-loan-application.service';
import { StaffApplicationSummary } from '../../../core/models/staff-application.model';

@Component({
  selector: 'los-staff-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="staff-dashboard">
      <!-- Header -->
      <div class="page-header">
        <div>
          <h1>Welcome, {{ displayName }}</h1>
          <p class="subtitle">{{ displayRole }} Dashboard</p>
        </div>
      </div>

      <!-- Stats row -->
      @if (!loading()) {
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-value">{{ total }}</div>
            <div class="stat-label">Total Applications</div>
          </div>
          <div class="stat-card pending">
            <div class="stat-value">{{ pending }}</div>
            <div class="stat-label">Awaiting My Review</div>
          </div>
          <div class="stat-card approved">
            <div class="stat-value">{{ approved }}</div>
            <div class="stat-label">Approved</div>
          </div>
          <div class="stat-card rejected">
            <div class="stat-value">{{ rejected }}</div>
            <div class="stat-label">Rejected</div>
          </div>
        </div>
      }

      <!-- Applications table -->
      <div class="section">
        <div class="section-header">
          <h2>Applications</h2>
          <div class="filter-tabs">
            <button [class.active]="filter() === 'all'" (click)="filter.set('all')">All</button>
            <button [class.active]="filter() === 'pending'" (click)="filter.set('pending')">
              My Stage
            </button>
            <button
              [class.active]="filter() === 'under_review'"
              (click)="filter.set('under_review')"
            >
              All Under Review
            </button>
          </div>
        </div>

        @if (loading()) {
          <div class="loading-row">Loading applications…</div>
        } @else if (error()) {
          <div class="error-banner" role="alert">{{ error() }}</div>
        } @else if (filteredApps().length === 0) {
          <div class="empty-state">No applications to show.</div>
        } @else {
          <div class="table-wrapper">
            <table class="app-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Applicant</th>
                  <th>Amount</th>
                  <th>Purpose</th>
                  <th>Status</th>
                  <th>Submitted</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (app of filteredApps(); track app.applicationRef) {
                  <tr
                    class="app-row"
                    (click)="openDetail(app.applicationRef)"
                    [attr.aria-label]="'Open ' + app.applicationRef"
                  >
                    <td class="ref-cell">{{ app.applicationRef }}</td>
                    <td>
                      {{
                        app.applicantName ??
                          (app.fineractClientId ? 'Client #' + app.fineractClientId : '—')
                      }}
                    </td>
                    <td class="amount-cell">
                      {{ app.requestedAmount | number: '1.2-2' }} {{ app.currency }}
                    </td>
                    <td>{{ app.loanPurpose ?? '—' }}</td>
                    <td>
                      <span class="status-badge" [class]="statusClass(app.status)">{{
                        app.status
                      }}</span>
                    </td>
                    <td class="date-cell">{{ app.createdAt | date: 'dd MMM yyyy' }}</td>
                    <td>
                      <button
                        class="view-btn"
                        (click)="$event.stopPropagation(); openDetail(app.applicationRef)"
                      >
                        View →
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    </div>
  `,
  styles: [
    `
      .staff-dashboard {
        max-width: 1200px;
      }

      .page-header {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        margin-bottom: 1.5rem;
      }

      h1 {
        font-size: 1.5rem;
        font-weight: 700;
        color: var(--color-text);
        margin: 0 0 0.2rem;
      }
      .subtitle {
        color: var(--color-text-muted);
        font-size: 0.9rem;
        margin: 0;
      }

      /* Stats */
      .stats-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
        gap: 1rem;
        margin-bottom: 1.75rem;
      }

      .stat-card {
        background: var(--color-surface);
        border: 1px solid var(--color-border);
        border-radius: 10px;
        padding: 1.1rem 1.25rem;
      }

      .stat-value {
        font-size: 1.9rem;
        font-weight: 700;
        color: var(--color-text);
      }
      .stat-label {
        font-size: 0.8rem;
        color: var(--color-text-muted);
        margin-top: 0.2rem;
      }

      .stat-card.pending .stat-value {
        color: #d97706;
      }
      .stat-card.approved .stat-value {
        color: #16a34a;
      }
      .stat-card.rejected .stat-value {
        color: #dc2626;
      }

      /* Section */
      .section {
        background: var(--color-surface);
        border: 1px solid var(--color-border);
        border-radius: 10px;
        overflow: hidden;
      }

      .section-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 1rem 1.25rem;
        border-bottom: 1px solid var(--color-border);
      }

      h2 {
        font-size: 1rem;
        font-weight: 600;
        color: var(--color-text);
        margin: 0;
      }

      .filter-tabs {
        display: flex;
        gap: 0.25rem;
      }

      .filter-tabs button {
        background: none;
        border: 1px solid var(--color-border);
        border-radius: 6px;
        padding: 0.3rem 0.75rem;
        font-size: 0.8rem;
        cursor: pointer;
        color: var(--color-text-muted);
      }

      .filter-tabs button.active {
        background: var(--color-primary);
        color: #fff;
        border-color: var(--color-primary);
      }

      /* Table */
      .table-wrapper {
        overflow-x: auto;
      }

      .app-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 0.875rem;
      }

      .app-table th {
        text-align: left;
        padding: 0.6rem 1rem;
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: var(--color-text-muted);
        border-bottom: 1px solid var(--color-border);
        white-space: nowrap;
      }

      .app-row {
        cursor: pointer;
        transition: background 0.1s;
      }

      .app-row:hover {
        background: var(--color-bg);
      }
      .app-row td {
        padding: 0.75rem 1rem;
        border-bottom: 1px solid var(--color-border);
        color: var(--color-text);
      }
      .app-row:last-child td {
        border-bottom: none;
      }

      .ref-cell {
        font-family: monospace;
        font-size: 0.8rem;
        font-weight: 600;
      }
      .amount-cell {
        font-weight: 600;
      }
      .date-cell {
        color: var(--color-text-muted);
        font-size: 0.82rem;
      }

      .status-badge {
        display: inline-block;
        padding: 0.18rem 0.6rem;
        border-radius: 99px;
        font-size: 0.72rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.03em;
      }

      .status-DRAFT {
        background: #f3f4f6;
        color: #6b7280;
      }
      .status-SUBMITTED {
        background: #dbeafe;
        color: #1d4ed8;
      }
      .status-UNDER_REVIEW {
        background: #fef9c3;
        color: #a16207;
      }
      .status-APPROVED {
        background: #dcfce7;
        color: #15803d;
      }
      .status-REJECTED {
        background: #fee2e2;
        color: #dc2626;
      }
      .status-REFERRED {
        background: #ede9fe;
        color: #7c3aed;
      }
      .status-DISBURSED {
        background: #d1fae5;
        color: #065f46;
      }

      .view-btn {
        background: none;
        border: 1px solid var(--color-border);
        border-radius: 6px;
        padding: 0.25rem 0.65rem;
        font-size: 0.8rem;
        cursor: pointer;
        color: var(--color-primary);
        white-space: nowrap;
      }

      .view-btn:hover {
        background: var(--color-primary);
        color: #fff;
      }

      .loading-row,
      .empty-state {
        padding: 2.5rem;
        text-align: center;
        color: var(--color-text-muted);
        font-size: 0.9rem;
      }

      .error-banner {
        margin: 1rem;
        background: var(--color-error-bg);
        color: var(--color-error);
        padding: 0.75rem 1rem;
        border-radius: 8px;
      }
    `,
  ],
})
export class StaffDashboardComponent implements OnInit {
  private readonly staffAuth = inject(StaffAuthService);
  private readonly staffSvc = inject(StaffLoanApplicationService);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly filter = signal<'all' | 'pending' | 'under_review'>('pending');

  private readonly apps = signal<StaffApplicationSummary[]>([]);

  get displayName(): string {
    return this.staffAuth.getProfile()?.username ?? 'there';
  }
  get displayRole(): string {
    return this.staffAuth.getProfile()?.displayRole ?? 'Staff';
  }
  get losRole(): string {
    return this.staffAuth.getProfile()?.losRole ?? '';
  }
  get total(): number {
    return this.apps().length;
  }
  get approved(): number {
    return this.apps().filter((a) => a.status === 'APPROVED').length;
  }
  get rejected(): number {
    return this.apps().filter((a) => a.status === 'REJECTED').length;
  }
  get pending(): number {
    const myStage = this.losRole;
    return this.apps().filter(
      (a) =>
        (a.status === 'UNDER_REVIEW' && a.currentApprovalStage === myStage) ||
        a.status === 'SUBMITTED',
    ).length;
  }

  filteredApps(): StaffApplicationSummary[] {
    const all = this.apps();
    const myStage = this.losRole;

    switch (this.filter()) {
      case 'pending':
        // Show apps at MY stage (UNDER_REVIEW) + SUBMITTED apps waiting for Start Review
        return all.filter(
          (a) =>
            (a.status === 'UNDER_REVIEW' && a.currentApprovalStage === myStage) ||
            a.status === 'SUBMITTED',
        );
      case 'under_review':
        // Show all under review regardless of stage
        return all.filter((a) => a.status === 'UNDER_REVIEW');
      default:
        return all;
    }
  }

  statusClass(status: string): string {
    return `status-badge status-${status}`;
  }

  ngOnInit(): void {
    this.staffSvc
      .getAll()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (apps) => this.apps.set(apps),
        error: (err) => this.error.set(err?.message ?? 'Could not load applications.'),
      });
  }

  openDetail(ref: string): void {
    this.router.navigate(['/staff/applications', ref]);
  }
}
