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
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { finalize, take } from 'rxjs';
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
    return this.applications.filter((a) => a.status !== 'DISBURSED' && a.status !== 'REJECTED')
      .length;
  }

  get disbursedCount(): number {
    return this.applications.filter((a) => a.status === 'DISBURSED').length;
  }

  ngOnInit(): void {
    this.customerLoanApplicationService
      .myApplications()
      .pipe(
        take(1),
        finalize(() => (this.loading = false)),
      )
      .subscribe({
        next: (apps) => {
          this.applications = apps;
        },
        error: (err) => {
          this.error = err?.message ?? 'Could not load your applications.';
        },
      });
  }
}
