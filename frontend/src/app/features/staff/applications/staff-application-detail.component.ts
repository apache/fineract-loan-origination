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
import { StaffAuthService } from '../../../core/services/staff-auth.service';
import { StaffLoanApplicationService } from '../../../core/services/staff-loan-application.service';
import { StaffApplicationDetail } from '../../../core/models/staff-application.model';
import { ApprovalDecisionType } from '../../../core/models/enums';

@Component({
  selector: 'los-staff-application-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="detail-page">

      <!-- Back -->
      <button class="back-btn" (click)="goBack()">← Back to Applications</button>

      @if (loading()) {
        <div class="loading">Loading application details…</div>
      } @else if (error()) {
        <div class="error-banner" role="alert">{{ error() }}</div>
      } @else if (app) {

        <!-- Title row -->
        <div class="page-header">
          <div>
            <h1>{{ app.applicationRef }}</h1>
            <span class="status-badge" [class]="'status-' + app.status">{{ app.status }}</span>
          </div>
        </div>

        <div class="detail-grid">

          <!-- Left column -->
          <div class="left-col">

            <!-- Application info -->
            <section class="card">
              <h2>Loan Details</h2>
              <dl class="detail-list">
                <div><dt>Amount</dt><dd>{{ app.requestedAmount | number:'1.2-2' }} {{ app.currency }}</dd></div>
                <div><dt>Purpose</dt><dd>{{ app.loanPurpose ?? '—' }}</dd></div>
                <div><dt>Tenor</dt><dd>{{ app.tenorMonths ? app.tenorMonths + ' months' : '—' }}</dd></div>
                <div><dt>Submitted</dt><dd>{{ app.submittedAt | date:'dd MMM yyyy, HH:mm' }}</dd></div>
                <div><dt>Last Updated</dt><dd>{{ app.updatedAt | date:'dd MMM yyyy, HH:mm' }}</dd></div>
                @if (app.fineractLoanId) {
                  <div><dt>Fineract Loan ID</dt><dd>{{ app.fineractLoanId }}</dd></div>
                }
                @if (app.disbursedAt) {
                  <div><dt>Disbursed At</dt><dd>{{ app.disbursedAt | date:'dd MMM yyyy, HH:mm' }}</dd></div>
                }
              </dl>
            </section>

            <!-- Applicant info -->
            <section class="card">
              <h2>Applicant</h2>
              <dl class="detail-list">
                <div><dt>Name</dt><dd>{{ app.applicantName }}</dd></div>
                @if (app.nationalId) {
                  <div><dt>National ID</dt><dd>{{ app.nationalId }}</dd></div>
                }
                @if (app.fineractClientId) {
                  <div><dt>Fineract Client ID</dt><dd>{{ app.fineractClientId }}</dd></div>
                }
                @if (app.monthlyIncome != null) {
                  <div><dt>Monthly Income</dt><dd>{{ app.monthlyIncome | number:'1.2-2' }} {{ app.currency }}</dd></div>
                }
                @if (app.employmentStatus) {
                  <div><dt>Employment</dt><dd>{{ app.employmentStatus }}</dd></div>
                }
                @if (app.employmentDurationMonths != null) {
                  <div><dt>Employment Duration</dt><dd>{{ app.employmentDurationMonths }} months</dd></div>
                }
                @if (app.existingLoanObligations != null) {
                  <div><dt>Existing Obligations</dt><dd>{{ app.existingLoanObligations | number:'1.2-2' }} {{ app.currency }}/mo</dd></div>
                }
              </dl>
            </section>

            <!-- Credit score -->
            @if (app.creditScore) {
              <section class="card">
                <h2>Credit Score</h2>
                <div class="score-display">
                  <div class="score-circle" [class]="'risk-' + app.creditScore.riskRating">
                    <span class="score-number">{{ app.creditScore.score }}</span>
                    <span class="score-label">/100</span>
                  </div>
                  <div class="risk-label" [class]="'risk-text-' + app.creditScore.riskRating">
                    {{ app.creditScore.riskRating }} RISK
                  </div>
                </div>
                <dl class="detail-list factors">
                  <div><dt>Income Ratio</dt><dd>{{ app.creditScore.incomeRatioScore }}/30</dd></div>
                  <div><dt>Debt Burden</dt><dd>{{ app.creditScore.debtBurdenScore }}/25</dd></div>
                  <div><dt>Employment</dt><dd>{{ app.creditScore.employmentScore }}/20</dd></div>
                  <div><dt>Repayment History</dt><dd>{{ app.creditScore.repaymentHistoryScore }}/15</dd></div>
                  <div><dt>Loan Purpose</dt><dd>{{ app.creditScore.loanPurposeScore }}/10</dd></div>
                </dl>
              </section>
            } @else if (app.status === 'SUBMITTED') {
              <section class="card">
                <h2>Credit Score</h2>
                <p class="muted">Not yet computed. Start review to trigger credit scoring.</p>
                <button class="action-btn secondary" [disabled]="startingReview()" (click)="startReview()">
                  {{ startingReview() ? 'Starting…' : 'Start Review' }}
                </button>
                @if (reviewError()) {
                  <div class="error-banner small" role="alert">{{ reviewError() }}</div>
                }
              </section>
            }

          </div>

          <!-- Right column -->
          <div class="right-col">

            <!-- Approval action panel -->
            @if (canDecide) {
              <section class="card action-card">
                <h2>Record Decision</h2>
                <p class="action-context">
                  Acting as <strong>{{ displayRole }}</strong> at stage <strong>{{ currentStage }}</strong>
                </p>

                <div class="decision-buttons">
                  <button
                    class="decision-btn approve"
                    [class.selected]="selectedDecision() === 'APPROVE'"
                    (click)="selectedDecision.set('APPROVE')"
                  >✓ Approve</button>
                  <button
                    class="decision-btn refer"
                    [class.selected]="selectedDecision() === 'REFER'"
                    (click)="selectedDecision.set('REFER')"
                  >↩ Refer Back</button>
                  <button
                    class="decision-btn reject"
                    [class.selected]="selectedDecision() === 'REJECT'"
                    (click)="selectedDecision.set('REJECT')"
                  >✗ Reject</button>
                </div>

                <div class="field">
                  <label for="comments">Comments <span class="required">*</span></label>
                  <textarea
                    id="comments"
                    [(ngModel)]="comments"
                    rows="4"
                    placeholder="Mandatory — explain your decision…"
                    [class.error]="commentsError()"
                  ></textarea>
                  @if (commentsError()) {
                    <span class="field-error">Comments are required.</span>
                  }
                </div>

                @if (decisionError()) {
                  <div class="error-banner small" role="alert">{{ decisionError() }}</div>
                }
                @if (decisionSuccess()) {
                  <div class="success-banner" role="status">Decision recorded successfully.</div>
                }

                <button
                  class="submit-decision-btn"
                  [disabled]="!selectedDecision() || submitting()"
                  (click)="submitDecision()"
                >
                  {{ submitting() ? 'Submitting…' : 'Submit Decision' }}
                </button>
              </section>
            }

            <!-- Approval history timeline -->
            <section class="card">
              <h2>Approval History</h2>
              @if (app.approvalStages.length === 0) {
                <p class="muted">No decisions recorded yet.</p>
              } @else {
                <ol class="timeline">
                  @for (stage of app.approvalStages; track stage.stage + stage.decidedAt) {
                    <li class="timeline-item" [class]="'decision-' + stage.decision">
                      <div class="timeline-dot" [class]="'dot-' + stage.decision"></div>
                      <div class="timeline-body">
                        <div class="timeline-header">
                          <span class="stage-name">{{ stageName(stage.stage) }}</span>
                          <span class="decision-pill" [class]="'pill-' + stage.decision">
                            {{ stage.decision ?? 'PENDING' }}
                          </span>
                        </div>
                        @if (stage.decidedBy) {
                          <div class="timeline-meta">by {{ stage.decidedBy }}
                            @if (stage.decidedAt) { · {{ stage.decidedAt | date:'dd MMM yyyy, HH:mm' }} }
                          </div>
                        }
                        @if (stage.notes) {
                          <div class="timeline-notes">"{{ stage.notes }}"</div>
                        }
                      </div>
                    </li>
                  }
                </ol>
              }
            </section>

          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .detail-page { max-width: 1100px; }

    .back-btn {
      background: none;
      border: none;
      color: var(--color-primary);
      cursor: pointer;
      font-size: 0.875rem;
      padding: 0;
      margin-bottom: 1.25rem;
    }

    .back-btn:hover { text-decoration: underline; }

    .page-header {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      margin-bottom: 1.5rem;
      flex-wrap: wrap;
    }

    h1 { font-size: 1.4rem; font-weight: 700; color: var(--color-text); margin: 0 0 0.4rem; }

    .status-badge {
      display: inline-block;
      padding: 0.25rem 0.75rem;
      border-radius: 99px;
      font-size: 0.78rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }

    .status-DRAFT        { background: #f3f4f6; color: #6b7280; }
    .status-SUBMITTED    { background: #dbeafe; color: #1d4ed8; }
    .status-UNDER_REVIEW { background: #fef9c3; color: #a16207; }
    .status-APPROVED     { background: #dcfce7; color: #15803d; }
    .status-REJECTED     { background: #fee2e2; color: #dc2626; }
    .status-REFERRED     { background: #ede9fe; color: #7c3aed; }
    .status-DISBURSED    { background: #d1fae5; color: #065f46; }

    /* Grid */
    .detail-grid {
      display: grid;
      grid-template-columns: 1fr 380px;
      gap: 1.25rem;
      align-items: start;
    }

    @media (max-width: 900px) {
      .detail-grid { grid-template-columns: 1fr; }
    }

    /* Card */
    .card {
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: 10px;
      padding: 1.25rem;
      margin-bottom: 1.25rem;
    }

    .card h2 {
      font-size: 0.9rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: var(--color-text-muted);
      margin: 0 0 1rem;
    }

    /* Detail list */
    .detail-list { display: grid; gap: 0.5rem; }
    .detail-list div { display: flex; gap: 0.5rem; }
    .detail-list dt { font-size: 0.8rem; color: var(--color-text-muted); min-width: 140px; flex-shrink: 0; }
    .detail-list dd { font-size: 0.875rem; color: var(--color-text); font-weight: 500; margin: 0; }
    .detail-list.factors dt { min-width: 130px; }

    /* Credit score */
    .score-display { display: flex; align-items: center; gap: 1.25rem; margin-bottom: 1rem; }

    .score-circle {
      width: 72px; height: 72px; border-radius: 50%;
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      border: 3px solid;
      flex-shrink: 0;
    }

    .risk-LOW    { border-color: #16a34a; color: #16a34a; }
    .risk-MEDIUM { border-color: #d97706; color: #d97706; }
    .risk-HIGH   { border-color: #dc2626; color: #dc2626; }

    .score-number { font-size: 1.4rem; font-weight: 700; line-height: 1; }
    .score-label  { font-size: 0.65rem; color: var(--color-text-muted); }

    .risk-label { font-size: 0.85rem; font-weight: 700; }
    .risk-text-LOW    { color: #16a34a; }
    .risk-text-MEDIUM { color: #d97706; }
    .risk-text-HIGH   { color: #dc2626; }

    /* Action card */
    .action-card { border: 2px solid var(--color-primary); }
    .action-context { font-size: 0.85rem; color: var(--color-text-muted); margin-bottom: 1rem; }

    .decision-buttons { display: flex; gap: 0.5rem; margin-bottom: 1.1rem; flex-wrap: wrap; }

    .decision-btn {
      flex: 1;
      padding: 0.55rem 0.5rem;
      border-radius: 8px;
      border: 2px solid var(--color-border);
      font-size: 0.85rem;
      font-weight: 600;
      cursor: pointer;
      background: var(--color-surface);
      color: var(--color-text-muted);
      transition: all 0.12s;
      min-width: 80px;
    }

    .decision-btn.approve.selected { background: #dcfce7; border-color: #16a34a; color: #15803d; }
    .decision-btn.refer.selected   { background: #ede9fe; border-color: #7c3aed; color: #6d28d9; }
    .decision-btn.reject.selected  { background: #fee2e2; border-color: #dc2626; color: #dc2626; }

    .decision-btn:hover { border-color: var(--color-primary); }

    .field { margin-bottom: 0.85rem; }
    .field label { display: block; font-size: 0.82rem; font-weight: 500; margin-bottom: 0.3rem; color: var(--color-text); }
    .required { color: var(--color-error); }

    textarea {
      width: 100%;
      padding: 0.55rem 0.75rem;
      border: 1px solid var(--color-border);
      border-radius: var(--radius);
      background: var(--color-bg);
      color: var(--color-text);
      font-size: 0.875rem;
      resize: vertical;
      font-family: inherit;
      box-sizing: border-box;
    }

    textarea:focus { outline: none; border-color: var(--color-primary); }
    textarea.error { border-color: var(--color-error); }
    .field-error { font-size: 0.78rem; color: var(--color-error); margin-top: 0.2rem; display: block; }

    .submit-decision-btn {
      width: 100%;
      padding: 0.65rem;
      background: var(--color-primary);
      color: #fff;
      border: none;
      border-radius: var(--radius);
      font-size: 0.9rem;
      font-weight: 600;
      cursor: pointer;
    }

    .submit-decision-btn:disabled { opacity: 0.55; cursor: not-allowed; }
    .submit-decision-btn:not(:disabled):hover { background: var(--color-primary-dark); }

    .action-btn {
      padding: 0.5rem 1rem;
      border-radius: var(--radius);
      font-size: 0.875rem;
      font-weight: 600;
      cursor: pointer;
      border: none;
    }

    .action-btn.secondary {
      background: var(--color-bg);
      border: 1px solid var(--color-border);
      color: var(--color-text);
    }

    /* Timeline */
    .timeline { list-style: none; padding: 0; margin: 0; position: relative; }
    .timeline::before {
      content: '';
      position: absolute;
      left: 10px; top: 0; bottom: 0;
      width: 2px;
      background: var(--color-border);
    }

    .timeline-item {
      display: flex;
      gap: 0.85rem;
      padding-bottom: 1.1rem;
      position: relative;
    }

    .timeline-dot {
      width: 20px; height: 20px; border-radius: 50%;
      border: 2px solid var(--color-border);
      background: var(--color-surface);
      flex-shrink: 0;
      z-index: 1;
      margin-top: 0.15rem;
    }

    .dot-APPROVE { border-color: #16a34a; background: #dcfce7; }
    .dot-REJECT  { border-color: #dc2626; background: #fee2e2; }
    .dot-REFER   { border-color: #7c3aed; background: #ede9fe; }

    .timeline-body { flex: 1; }

    .timeline-header { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.2rem; }

    .stage-name { font-size: 0.875rem; font-weight: 600; color: var(--color-text); }

    .decision-pill {
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.15rem 0.5rem;
      border-radius: 99px;
      text-transform: uppercase;
    }

    .pill-APPROVE { background: #dcfce7; color: #15803d; }
    .pill-REJECT  { background: #fee2e2; color: #dc2626; }
    .pill-REFER   { background: #ede9fe; color: #7c3aed; }
    .pill-null    { background: #f3f4f6; color: #6b7280; }

    .timeline-meta { font-size: 0.78rem; color: var(--color-text-muted); margin-bottom: 0.3rem; }

    .timeline-notes {
      font-size: 0.82rem;
      color: var(--color-text);
      font-style: italic;
      background: var(--color-bg);
      border-left: 3px solid var(--color-border);
      padding: 0.35rem 0.65rem;
      border-radius: 0 4px 4px 0;
    }

    /* Feedback banners */
    .error-banner {
      background: var(--color-error-bg);
      color: var(--color-error);
      padding: 0.65rem 0.85rem;
      border-radius: 8px;
      font-size: 0.875rem;
      margin-bottom: 0.75rem;
    }

    .error-banner.small { font-size: 0.8rem; }

    .success-banner {
      background: #dcfce7;
      color: #15803d;
      padding: 0.65rem 0.85rem;
      border-radius: 8px;
      font-size: 0.875rem;
      margin-bottom: 0.75rem;
    }

    .loading {
      padding: 3rem;
      text-align: center;
      color: var(--color-text-muted);
    }

    .muted { color: var(--color-text-muted); font-size: 0.875rem; }
  `],
})
export class StaffApplicationDetailComponent implements OnInit {
  private readonly staffAuth = inject(StaffAuthService);
  private readonly staffSvc  = inject(StaffLoanApplicationService);
  private readonly route     = inject(ActivatedRoute);
  private readonly router    = inject(Router);

  readonly loading        = signal(true);
  readonly error          = signal<string | null>(null);
  readonly startingReview = signal(false);
  readonly reviewError    = signal<string | null>(null);
  readonly submitting     = signal(false);
  readonly decisionError  = signal<string | null>(null);
  readonly decisionSuccess = signal(false);
  readonly commentsError  = signal(false);
  readonly selectedDecision = signal<ApprovalDecisionType | null>(null);

  app: StaffApplicationDetail | null = null;
  comments = '';

  get displayRole(): string  { return this.staffAuth.getProfile()?.displayRole ?? ''; }
  get losRole(): string      { return this.staffAuth.getProfile()?.losRole ?? ''; }

  /** The stage name this role is responsible for, e.g. LOAN_OFFICER */
  get currentStage(): string {
    return this.losRole.replace('ROLE_', '');
  }

  /** True when the application is UNDER_REVIEW and this user hasn't decided yet */
  get canDecide(): boolean {
    if (!this.app || this.app.status !== 'UNDER_REVIEW') return false;
    const username = this.staffAuth.getProfile()?.username;
    const alreadyDecided = this.app.approvalStages.some(
      s => s.decidedBy === username && s.decision != null,
    );
    return !alreadyDecided;
  }

  stageName(stage: string): string {
    return stage.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }

  ngOnInit(): void {
    const ref = this.route.snapshot.paramMap.get('applicationRef')!;
    this.loadDetail(ref);
  }

  private loadDetail(ref: string): void {
    this.loading.set(true);
    this.staffSvc
      .getDetail(ref)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next:  (detail) => this.app = detail,
        error: (err)    => this.error.set(err?.message ?? 'Could not load application.'),
      });
  }

  startReview(): void {
    if (!this.app) return;
    this.startingReview.set(true);
    this.reviewError.set(null);
    this.staffSvc.startReview(this.app.applicationRef)
      .pipe(finalize(() => this.startingReview.set(false)))
      .subscribe({
        next:  () => this.loadDetail(this.app!.applicationRef),
        error: (err) => this.reviewError.set(err?.error?.message ?? 'Could not start review.'),
      });
  }

  submitDecision(): void {
    const decision = this.selectedDecision();
    if (!decision || !this.app) return;

    if (!this.comments.trim()) {
      this.commentsError.set(true);
      return;
    }

    this.commentsError.set(false);
    this.submitting.set(true);
    this.decisionError.set(null);
    this.decisionSuccess.set(false);

    this.staffSvc
      .recordDecision(this.app.applicationRef, { decision, comments: this.comments.trim() })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          this.decisionSuccess.set(true);
          this.comments = '';
          this.selectedDecision.set(null);
          this.loadDetail(this.app!.applicationRef);
        },
        error: (err) => {
          this.decisionError.set(
            err?.error?.message ?? 'Could not record decision. Check the application stage.',
          );
        },
      });
  }

  goBack(): void {
    this.router.navigate(['/staff/applications']);
  }
}
