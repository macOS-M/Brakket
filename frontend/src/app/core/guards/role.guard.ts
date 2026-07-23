import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

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

  if (requiredRoles.length === 0) {
    return true;
  }

  const authorize = () => authService.hasRole(...requiredRoles)
    ? true
    : router.createUrlTree(['/']);

  // Después de un F5 puede existir JWT, pero /me todavía no terminó. Esperar el
  // perfil evita rechazar rutas administrativas por una lista de roles temporalmente vacía.
  if (authService.usuario()) {
    return authorize();
  }

  return authService.loadCurrentUser().pipe(
    map(() => authorize()),
    catchError(() => of(router.createUrlTree(['/login'])))
  );
};
