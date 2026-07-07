import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { TokenService } from '../services/token.service';

/**
 * Interceptor funcional que agrega la cabecera
 * `Authorization: Bearer <token>` a cada peticion cuando existe un JWT.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const token = tokenService.getToken();

  if (!token) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });

  return next(authReq);
};
