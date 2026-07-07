import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { TokenService } from '../services/token.service';

/**
 * Protege rutas que requieren sesion iniciada.
 * Si no hay un JWT valido, redirige a /login.
 */
export const authGuard: CanActivateFn = () => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  if (tokenService.hasToken()) {
    return true;
  }

  return router.createUrlTree(['/login']);
};
