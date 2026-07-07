import { Injectable } from '@angular/core';

/**
 * Almacena, lee y borra el JWT emitido por el backend tras el login con Google.
 * Se persiste en localStorage para sobrevivir recargas de pagina.
 */
@Injectable({ providedIn: 'root' })
export class TokenService {
  private static readonly STORAGE_KEY = 'brakket.jwt';

  getToken(): string | null {
    return localStorage.getItem(TokenService.STORAGE_KEY);
  }

  setToken(token: string): void {
    localStorage.setItem(TokenService.STORAGE_KEY, token);
  }

  clear(): void {
    localStorage.removeItem(TokenService.STORAGE_KEY);
  }

  hasToken(): boolean {
    return !!this.getToken();
  }
}
