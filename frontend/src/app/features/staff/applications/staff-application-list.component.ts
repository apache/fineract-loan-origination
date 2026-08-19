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
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { StaffLoanApplicationService } from '../../../core/services/staff-loan-application.service';
import { StaffApplicationSummary } from '../../../core/models/staff-application.model';
import { LoanApplicationStatus } from '../../../core/models/enums';

@Component({
  selector: 'los-staff-application-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <h1>Loan Applications</h1>
      </div>

      <!-- Search + filter bar -->
      <div class="toolbar">
        <input
          class="search-input"
          type="search"
          [(ngModel)]="searchTerm"
          placeholder="Search by reference…"
          aria-label="Search applications"
        />
        <select class="status-select" [(ngModel)]="selectedStatus" aria-label="Filter by status">
          <option value="">All statuses</option>
          @for (s of allStatuses; track s) {
            <option [value]="s">{{ s }}</option>
          }
        </select>
      </div>

      @if (loading()) {
        <div class="loading">Loading applications…</div>
      } @else if (error()) {
        <div class="error-banner" role="alert">{{ error() }}</div>
      } @else if (filteredApps().length === 0) {
        <div class="empty">No applications match your filters.</div>
      } @else {
        <div class="table-wrapper">
          <table class="app-table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Applicant</th>
                <th>Amount</th>
                <th>Currency</th>
                <th>Purpose</th>
                <th>Tenor</th>
                <th>Status</th>
                <th>Submitted</th>
                <th>Updated</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (app of filteredApps(); track app.applicationRef) {
                <tr class="app-row" (click)="openDetail(app.applicationRef)">
                  <td class="ref-cell">{{ app.applicationRef }}</td>
                  <td>
                    {{
                      app.applicantName ??
                        (app.fineractClientId ? 'Client #' + app.fineractClientId : '—')
                    }}
                  </td>
                  <td class="amount-cell">{{ app.requestedAmount | number: '1.2-2' }}</td>
                  <td>{{ app.currency }}</td>
                  <td>{{ app.loanPurpose ?? '—' }}</td>
                  <td>{{ app.tenorMonths ? app.tenorMonths + ' mo' : '—' }}</td>
                  <td>
                    <span class="status-badge" [class]="'status-' + app.status">{{
                      app.status
                    }}</span>
                  </td>
                  <td class="date-cell">{{ app.createdAt | date: 'dd MMM yyyy' }}</td>
                  <td class="date-cell">{{ app.updatedAt | date: 'dd MMM yyyy' }}</td>
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
        <div class="count-label">
          Showing {{ filteredApps().length }} of {{ apps().length }} applications
        </div>
      }
    </div>
  `,
  styles: [
    `
      .page {
        max-width: 1200px;
      }
      .page-header {
        margin-bottom: 1.25rem;
      }
      h1 {
        font-size: 1.4rem;
        font-weight: 700;
        color: var(--color-text);
        margin: 0;
      }

      .toolbar {
        display: flex;
        gap: 0.75rem;
        margin-bottom: 1rem;
        flex-wrap: wrap;
      }

      .search-input,
      .status-select {
        padding: 0.5rem 0.85rem;
        border: 1px solid var(--color-border);
        border-radius: var(--radius);
        background: var(--color-surface);
        color: var(--color-text);
        font-size: 0.875rem;
        outline: none;
      }

      .search-input {
        flex: 1;
        min-width: 180px;
      }
      .search-input:focus,
      .status-select:focus {
        border-color: var(--color-primary);
      }

      .table-wrapper {
        background: var(--color-surface);
        border: 1px solid var(--color-border);
        border-radius: 10px;
        overflow: hidden;
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
        font-weight: 600;
        font-size: 0.8rem;
      }
      .amount-cell {
        font-weight: 600;
      }
      .date-cell {
        color: var(--color-text-muted);
        font-size: 0.82rem;
        white-space: nowrap;
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

      .count-label {
        font-size: 0.8rem;
        color: var(--color-text-muted);
        margin-top: 0.75rem;
      }

      .loading,
      .empty {
        background: var(--color-surface);
        border: 1px solid var(--color-border);
        border-radius: 10px;
        padding: 2.5rem;
        text-align: center;
        color: var(--color-text-muted);
      }

      .error-banner {
        background: var(--color-error-bg);
        color: var(--color-error);
        padding: 0.75rem 1rem;
        border-radius: 8px;
      }
    `,
  ],
})
export class StaffApplicationListComponent implements OnInit {
  private readonly staffSvc = inject(StaffLoanApplicationService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly apps = signal<StaffApplicationSummary[]>([]);

  searchTerm = '';
  selectedStatus = '';

  readonly allStatuses: LoanApplicationStatus[] = [
    'DRAFT',
    'SUBMITTED',
    'UNDER_REVIEW',
    'APPROVED',
    'REJECTED',
    'REFERRED',
    'DISBURSED',
  ];

  filteredApps(): StaffApplicationSummary[] {
    return this.apps().filter((a) => {
      const matchSearch =
        !this.searchTerm || a.applicationRef.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchStatus = !this.selectedStatus || a.status === this.selectedStatus;
      return matchSearch && matchStatus;
    });
  }

  ngOnInit(): void {
    // Pre-select filter from query params if navigated from dashboard
    this.route.queryParamMap.subscribe((params) => {
      const f = params.get('filter');
      if (f === 'pending') this.selectedStatus = 'UNDER_REVIEW';
    });

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
