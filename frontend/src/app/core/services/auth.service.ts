import { Injectable, computed, inject, signal } from '@angular/core';

import { Usuario } from '../models/usuario.model';
import { ApiService } from './api.service';

/**
 * Gestiona el estado de autenticacion del usuario.
 * El login/logout se delegan al backend (flujo OAuth2 con Google), por lo que
 * usamos redirecciones de navegador completas en lugar de peticiones AJAX.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);

  private readonly backendBaseUrl = 'http://localhost:8080';

  /** Usuario actual (null mientras no se ha cargado). */
  private readonly usuarioSignal = signal<Usuario | null>(null);
  readonly usuario = this.usuarioSignal.asReadonly();

  readonly isAuthenticated = computed(() => this.usuarioSignal()?.authenticated ?? false);

  /** Carga el usuario autenticado desde el backend (GET /me). */
  loadCurrentUser(): void {
    this.api.get<Usuario>('/me').subscribe({
      next: (usuario) => this.usuarioSignal.set(usuario),
      error: () => this.usuarioSignal.set({ authenticated: false })
    });
  }

  /** Redirige al flujo de autorizacion de Google gestionado por el backend. */
  login(): void {
    window.location.href = `${this.backendBaseUrl}/oauth2/authorization/google`;
  }

  /** Cierra la sesion en el backend. */
  logout(): void {
    this.usuarioSignal.set(null);
    window.location.href = `${this.backendBaseUrl}/logout`;
  }
}
