import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

/**
 * Protege rutas segun rol. Se configura con `data: { roles: ['ADMIN', ...] }`.
 * Si el usuario no posee ninguno de los roles requeridos, redirige a home.
 *
 * Placeholder funcional: la validacion definitiva depende del payload real del
 * JWT / del endpoint /me. Pendiente EPIC-15.
 */
export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const requiredRoles = (route.data?.['roles'] as string[] | undefined) ?? [];

  if (requiredRoles.length === 0 || authService.hasRole(...requiredRoles)) {
    return true;
  }

  return router.createUrlTree(['/']);
};
