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
