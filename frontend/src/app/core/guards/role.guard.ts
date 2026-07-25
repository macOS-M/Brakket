import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Protege rutas según rol. Los roles llegan de forma asíncrona desde GET /me,
 * por lo que tras un F5 el guard espera la carga del perfil antes de decidir.
 */
export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requiredRoles = (route.data?.['roles'] as string[] | undefined) ?? [];

  if (requiredRoles.length === 0) {
    return true;
  }

  const evaluar = () =>
    authService.hasRole(...requiredRoles) ? true : router.createUrlTree(['/inicio']);

  if (authService.usuario() !== null) {
    return evaluar();
  }

  return firstValueFrom(authService.loadCurrentUser()).then(
    evaluar,
    () => router.createUrlTree(['/login'])
  );
};
