import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { TokenService } from '../services/token.service';

/**
 * Interceptor funcional que agrega la cabecera
 * `Authorization: Bearer <token>` a cada peticion cuando existe un JWT.
 *
 * Ademas salta las paginas de aviso que los tuneles de demo interponen en la
 * primera visita de cada navegador y que devolverian HTML en vez del JSON:
 *   - `ngrok-skip-browser-warning` para ngrok.
 *   - `X-Tunnel-Skip-AntiPhishing-Page` para el Port Forwarding de VS Code
 *     (dev tunnels de Microsoft, dominios *.devtunnels.ms).
 * Van en TODAS las peticiones, haya token o no. En local son inofensivas.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const token = tokenService.getToken();

  const cabeceras: Record<string, string> = {
    'ngrok-skip-browser-warning': 'true',
    'X-Tunnel-Skip-AntiPhishing-Page': 'true'
  };
  if (token) {
    cabeceras['Authorization'] = `Bearer ${token}`;
  }

  return next(req.clone({ setHeaders: cabeceras }));
};
