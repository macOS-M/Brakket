import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Protege rutas segun rol. Se configura con `data: { roles: ['ADMIN', ...] }`.
 * Si el usuario no posee ninguno de los roles requeridos, redirige a home.
 *
 * Los roles llegan asincrono desde GET /me: tras un F5 (o navegando directo
 * por URL) el guard corre antes de que la respuesta vuelva. Si todavia no hay
 * usuario cargado, espera esa carga antes de decidir; sin esto, un admin que
 * entra directo a /admin rebotaria a home aunque tenga el rol.
 */
export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const requiredRoles = (route.data?.['roles'] as string[] | undefined) ?? [];
  if (requiredRoles.length === 0) {
    return true;
  }

  // Rebota al dashboard (/inicio): la raíz ahora es el landing público.
  const evaluar = () =>
    authService.hasRole(...requiredRoles) ? true : router.createUrlTree(['/inicio']);

  if (authService.usuario() !== null) {
    return evaluar();
  }

  return firstValueFrom(authService.loadCurrentUser()).then(evaluar, () =>
    router.createUrlTree(['/inicio'])
  );
};
