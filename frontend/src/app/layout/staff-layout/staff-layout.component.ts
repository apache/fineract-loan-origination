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
import { StaffAuthService, StaffProfile } from '../../core/services/staff-auth.service';

const SIDEBAR_KEY = 'los-staff-sidebar-collapsed';

@Component({
  selector: 'los-staff-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="shell">

      <!-- ── Sidebar ── -->
      <aside class="sidebar" [class.collapsed]="collapsed()">

        <!-- Brand -->
        <div class="brand">
          <img src="fineract_logo.png" alt="Apache Fineract" class="brand-logo" />
          @if (!collapsed()) {
            <span class="brand-name">LOS Staff</span>
          }
          <button
            class="collapse-toggle"
            type="button"
            (click)="toggleSidebar()"
            [attr.aria-label]="collapsed() ? 'Expand sidebar' : 'Collapse sidebar'"
          >
            <svg viewBox="0 0 16 16" width="14" height="14" [class.flipped]="collapsed()">
              <path d="M10 2 L5 8 L10 14"
                stroke="currentColor" stroke-width="1.6"
                fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>

        <!-- Role pill (expanded only) -->
        @if (!collapsed() && profile) {
          <div class="role-pill" [class]="rolePillClass">
            {{ profile.displayRole }}
          </div>
        }

        <!-- Nav -->
        <nav class="nav">

          <a routerLink="/staff/dashboard" routerLinkActive="active"
             [routerLinkActiveOptions]="{exact:true}" class="nav-item"
             [attr.data-tooltip]="collapsed() ? 'Dashboard' : null">
            <span class="nav-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none"
                   stroke="currentColor" stroke-width="1.8">
                <rect x="3" y="3" width="7" height="7" rx="1"/>
                <rect x="14" y="3" width="7" height="7" rx="1"/>
                <rect x="3" y="14" width="7" height="7" rx="1"/>
                <rect x="14" y="14" width="7" height="7" rx="1"/>
              </svg>
            </span>
            @if (!collapsed()) { <span class="nav-label">Dashboard</span> }
          </a>

          <a routerLink="/staff/applications" routerLinkActive="active"
             [routerLinkActiveOptions]="{exact:true}" class="nav-item"
             [attr.data-tooltip]="collapsed() ? 'Applications' : null">
            <span class="nav-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none"
                   stroke="currentColor" stroke-width="1.8">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <path d="M14 2v6h6M8 13h8M8 17h8M8 9h2"/>
              </svg>
            </span>
            @if (!collapsed()) { <span class="nav-label">Applications</span> }
          </a>

          <a routerLink="/staff/applications" [queryParams]="{filter:'pending'}" class="nav-item"
             [attr.data-tooltip]="collapsed() ? 'Pending Review' : null">
            <span class="nav-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none"
                   stroke="currentColor" stroke-width="1.8">
                <circle cx="12" cy="12" r="10"/>
                <path d="M12 6v6l4 2"/>
              </svg>
            </span>
            @if (!collapsed()) { <span class="nav-label">Pending Review</span> }
          </a>

        </nav>

        <!-- Footer -->
        <div class="sidebar-footer">
          @if (profile && !collapsed()) {
            <div class="profile-chip">
              <span class="profile-name">{{ profile.username }}</span>
              <span class="profile-role">{{ profile.displayRole }}</span>
            </div>
          }
          <button class="logout-button" (click)="logout()"
                  [attr.data-tooltip]="collapsed() ? 'Sign out' : null">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none"
                 stroke="currentColor" stroke-width="1.8" aria-hidden="true">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <path d="M16 17l5-5-5-5"/>
              <path d="M21 12H9"/>
            </svg>
            @if (!collapsed()) {
              <span>Sign out</span>
            }
          </button>
        </div>

      </aside>

      <!-- ── Main content ── -->
      <main class="content">
        <router-outlet />
      </main>

    </div>
  `,
  styles: [`
    /* ── Shell ── */
    .shell {
      display: flex;
      min-height: 100vh;
    }

    /* ── Sidebar ── */
    .sidebar {
      width: 240px;
      flex-shrink: 0;
      background: var(--color-surface);
      border-right: 1px solid var(--color-border);
      display: flex;
      flex-direction: column;
      padding: 1.25rem 0;
      transition: width 0.2s ease;
      overflow: hidden;

      &.collapsed {
        width: 64px;

        .brand {
          padding: 0 0 1.25rem;
          justify-content: center;
          position: relative;

          .brand-logo { height: 24px; }

          .brand-name { display: none; }

          .collapse-toggle {
            position: absolute;
            right: -11px;
            top: 0;
            background: var(--color-surface);
          }
        }

        .nav {
          padding: 0 0.75rem;
          align-items: stretch;
        }

        .nav-item {
          justify-content: center;
          padding: 0.6rem 0;
          gap: 0;
        }

        .sidebar-footer {
          padding: 1rem 0.75rem 0;
          align-items: center;
        }

        .logout-button {
          width: 40px;
          height: 40px;
          justify-content: center;
          padding: 0;
        }
      }
    }

    /* ── Brand ── */
    .brand {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      padding: 0 1.25rem 1.25rem;
      border-bottom: 1px solid var(--color-border);
      margin-bottom: 0.75rem;
      position: relative;
    }

    .brand-logo {
      height: 28px;
      width: auto;
      object-fit: contain;
      flex-shrink: 0;
    }

    .brand-name {
      font-weight: 600;
      color: var(--color-text);
      white-space: nowrap;
      overflow: hidden;
    }

    .collapse-toggle {
      margin-left: auto;
      background: none;
      border: 1px solid var(--color-border);
      border-radius: 999px;
      width: 22px;
      height: 22px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: var(--color-text-muted);
      flex-shrink: 0;

      &:hover {
        border-color: var(--color-primary);
        color: var(--color-primary-dark);
      }

      svg { transition: transform 0.15s ease; }
      svg.flipped { transform: rotate(180deg); }
    }

    /* ── Role pill ── */
    .role-pill {
      margin: 0 1.25rem 0.75rem;
      padding: 0.22rem 0.75rem;
      border-radius: 99px;
      font-size: 0.7rem;
      font-weight: 700;
      text-align: center;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .loan-officer     { background: #dbeafe; color: #1d4ed8; }
    .credit-committee { background: #fef9c3; color: #a16207; }
    .branch-manager   { background: #dcfce7; color: #15803d; }

    /* ── Nav ── */
    .nav {
      display: flex;
      flex-direction: column;
      gap: 0.15rem;
      padding: 0 0.75rem;
      flex: 1;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 0.65rem;
      padding: 0.55rem 0.75rem;
      border-radius: 6px;
      color: var(--color-text-muted);
      text-decoration: none;
      font-size: 0.92rem;
      position: relative;

      &:hover {
        background: var(--color-primary-light);
        color: var(--color-text);
      }

      &.active {
        background: var(--color-primary-light);
        color: var(--color-primary-dark);
        font-weight: 600;
      }
    }

    .nav-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .nav-label {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    /* ── Footer ── */
    .sidebar-footer {
      padding: 1rem 1.25rem 0;
      border-top: 1px solid var(--color-border);
      margin-top: 1rem;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .profile-chip {
      display: flex;
      flex-direction: column;
    }

    .profile-name {
      font-weight: 600;
      color: var(--color-text);
      font-size: 0.9rem;
    }

    .profile-role {
      color: var(--color-text-muted);
      font-size: 0.78rem;
    }

    .logout-button {
      display: flex;
      align-items: center;
      justify-content: flex-start;
      gap: 0.5rem;
      width: 100%;
      background: none;
      border: 1px solid var(--color-border);
      border-radius: 6px;
      padding: 0.5rem 0.75rem;
      cursor: pointer;
      color: var(--color-text-muted);
      font: inherit;
      font-size: 0.88rem;

      &:hover {
        border-color: var(--color-primary);
        color: var(--color-primary-dark);
      }
    }

    /* ── Tooltips (match customer sidebar exactly) ── */
    .nav-item, .logout-button {
      position: relative;

      &[data-tooltip]::after {
        content: attr(data-tooltip);
        position: absolute;
        left: calc(100% + 10px);
        top: 50%;
        transform: translateY(-50%);
        background: var(--color-text);
        color: var(--color-surface);
        padding: 0.3rem 0.6rem;
        border-radius: 4px;
        font-size: 0.78rem;
        white-space: nowrap;
        opacity: 0;
        pointer-events: none;
        transition: opacity 0.1s ease;
        z-index: 20;
      }

      &:hover[data-tooltip]::after {
        opacity: 1;
      }
    }

    /* ── Content ── */
    .content {
      flex: 1;
      padding: 2rem;
      overflow-y: auto;
    }

    @media (max-width: 768px) {
      .sidebar { width: 64px; }
      .content { padding: 1rem; }
    }
  `],
})
export class StaffLayoutComponent implements OnInit {
  private readonly staffAuth = inject(StaffAuthService);
  private readonly router    = inject(Router);

  profile: StaffProfile | null = null;
  readonly collapsed = signal(this.readCollapsed());

  get rolePillClass(): string {
    const role = this.profile?.losRole ?? '';
    if (role.includes('LOAN_OFFICER'))     return 'role-pill loan-officer';
    if (role.includes('CREDIT_COMMITTEE')) return 'role-pill credit-committee';
    if (role.includes('BRANCH_MANAGER'))   return 'role-pill branch-manager';
    return 'role-pill';
  }

  ngOnInit(): void {
    this.profile = this.staffAuth.getProfile();
  }

  toggleSidebar(): void {
    const next = !this.collapsed();
    this.collapsed.set(next);
    try { localStorage.setItem(SIDEBAR_KEY, String(next)); } catch { /* ignore */ }
  }

  logout(): void {
    this.staffAuth.logout();
    this.router.navigate(['/staff/login']);
  }

  private readCollapsed(): boolean {
    try { return localStorage.getItem(SIDEBAR_KEY) === 'true'; } catch { return false; }
  }
}
