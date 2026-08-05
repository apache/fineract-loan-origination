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
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService, CustomerProfile } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';

interface NavItem {
  label: string;
  path: string;
  enabled: boolean;
  icon: string;
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
    { label: 'Dashboard', path: '/dashboard', enabled: true, icon: 'grid' },
    { label: 'Loan Applications', path: '/loan-application', enabled: true, icon: 'file-text' },
    { label: 'Documents', path: '/documents', enabled: false, icon: 'folder' },
    { label: 'My Loans', path: '/my-loans', enabled: true, icon: 'layers' },
    { label: 'Profile', path: '/profile', enabled: true, icon: 'user' },
    { label: 'Notifications', path: '/notifications', enabled: false, icon: 'bell' },
    { label: 'Support', path: '/support', enabled: true, icon: 'life-buoy' },
    { label: 'Settings', path: '/settings', enabled: true, icon: 'settings' },
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
