import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/** Attaches the Basic Auth header to every request once the user has logged in. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const authHeader = authService.getAuthHeader();

  if (!authHeader) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: authHeader } }));
};