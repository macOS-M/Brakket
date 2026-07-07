import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { TokenService } from '../services/token.service';

/**
 * Interceptor funcional de manejo de errores HTTP:
 *  - Ante un 401 limpia el token y redirige a /login.
 *  - Registra en consola cualquier otro error.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const tokenService = inject(TokenService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        tokenService.clear();
        router.navigate(['/login']);
      } else {
        console.error(`HTTP ${error.status} en ${req.url}:`, error.message);
      }
      return throwError(() => error);
    })
  );
};
