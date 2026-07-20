import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'los-settings',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  profile = this.authService.getProfile();

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}