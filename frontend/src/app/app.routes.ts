import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
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
  { path: '**', redirectTo: 'dashboard' },
];
