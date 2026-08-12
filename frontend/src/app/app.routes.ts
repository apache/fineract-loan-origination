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
import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { StaffLayoutComponent } from './layout/staff-layout/staff-layout.component';
import { authGuard } from './core/guards/auth.guard';
import { staffAuthGuard } from './core/guards/staff-auth.guard';

export const routes: Routes = [
  // -----------------------------------------------------------------------
  // Customer / client routes
  // -----------------------------------------------------------------------
  { path: 'login', component: LoginComponent },

  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'apply',
        loadComponent: () =>
          import('./features/loan-application/loan-application-create.component').then(
            (m) => m.LoanApplicationCreateComponent,
          ),
      },
      {
        path: 'loan-application',
        loadComponent: () =>
          import('./features/loan-application/loan-application-list.component').then(
            (m) => m.LoanApplicationListComponent,
          ),
      },
      {
        path: 'loan-application/:applicationRef',
        loadComponent: () =>
          import('./features/loan-application/loan-application-detail.component').then(
            (m) => m.LoanApplicationDetailComponent,
          ),
      },
      {
        path: 'my-loans',
        loadComponent: () =>
          import('./features/my-loans/my-loans.component').then((m) => m.MyLoansComponent),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.component').then((m) => m.ProfileComponent),
      },
      {
        path: 'support',
        loadComponent: () =>
          import('./features/support/support.component').then((m) => m.SupportComponent),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings.component').then((m) => m.SettingsComponent),
      },
    ],
  },

  // -----------------------------------------------------------------------
  // Staff routes  (Loan Officer | Credit Committee | Branch Manager)
  // -----------------------------------------------------------------------
  {
    path: 'staff/login',
    loadComponent: () =>
      import('./features/staff/login/staff-login.component').then((m) => m.StaffLoginComponent),
  },

  {
    path: 'staff',
    component: StaffLayoutComponent,
    canActivate: [staffAuthGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/staff/dashboard/staff-dashboard.component').then(
            (m) => m.StaffDashboardComponent,
          ),
      },

      {
        path: 'applications',
        loadComponent: () =>
          import('./features/staff/applications/staff-application-list.component').then(
            (m) => m.StaffApplicationListComponent,
          ),
      },

      {
        path: 'applications/:applicationRef',
        loadComponent: () =>
          import('./features/staff/applications/staff-application-detail.component').then(
            (m) => m.StaffApplicationDetailComponent,
          ),
      },
    ],
  },

  // Catch-all — send unknown URLs to login, not dashboard (dashboard is protected)
  { path: '**', redirectTo: 'login' },
];
