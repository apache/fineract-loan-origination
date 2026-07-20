import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService, CustomerProfile } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';

interface NavItem {
  label: string;
  path: string;
  enabled: boolean;
}

const SIDEBAR_STORAGE_KEY = 'los-sidebar-collapsed';

@Component({
  selector: 'los-main-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
})
export class MainLayoutComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly themeService = inject(ThemeService);

  profile: CustomerProfile | null = null;
  sidebarCollapsed = signal(this.readInitialCollapsedState());

  readonly navItems: NavItem[] = [
    { label: 'Dashboard', path: '/dashboard', enabled: true },
    { label: 'Loan Applications', path: '/loan-application', enabled: true },
    { label: 'Documents', path: '/documents', enabled: false },
    { label: 'My Loans', path: '/my-loans', enabled: true },
    { label: 'Profile', path: '/profile', enabled: true },
    { label: 'Notifications', path: '/notifications', enabled: false },
    { label: 'Support', path: '/support', enabled: true },
    { label: 'Settings', path: '/settings', enabled: true },
  ];

  ngOnInit(): void {
    this.profile = this.authService.getProfile();
  }

  toggleSidebar(): void {
    const next = !this.sidebarCollapsed();
    this.sidebarCollapsed.set(next);
    try {
      localStorage.setItem(SIDEBAR_STORAGE_KEY, String(next));
    } catch {
      // localStorage unavailable — collapsed state just won't persist across reloads.
    }
  }

  toggleTheme(): void {
    this.themeService.toggle();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private readInitialCollapsedState(): boolean {
    try {
      return localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true';
    } catch {
      return false;
    }
  }
}