import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { TokenService } from '../services/token.service';

/**
 * Interceptor funcional que agrega la cabecera
 * `Authorization: Bearer <token>` a cada peticion cuando existe un JWT.
 *
 * Ademas manda `ngrok-skip-browser-warning` en TODAS las peticiones (haya
 * token o no): cuando el backend se sirve por un tunel gratuito de ngrok para
 * la demo, la primera visita de cada navegador recibe una pagina HTML de aviso
 * en vez de la respuesta real. Cualquier cabecera propia salta ese aviso y
 * deja pasar el JSON. En local es inofensiva.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const token = tokenService.getToken();

  const cabeceras: Record<string, string> = { 'ngrok-skip-browser-warning': 'true' };
  if (token) {
    cabeceras['Authorization'] = `Bearer ${token}`;
  }

  return next(req.clone({ setHeaders: cabeceras }));
};
