import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';

import { Usuario } from '../../models/usuario.model';
import { ApiService } from './api.service';
import { TokenService } from './token.service';

/**
 * Gestiona el estado de autenticacion del usuario.
 *
 * Flujo: el login redirige al backend, que gestiona OAuth2 con Google y, tras
 * el callback, devuelve un JWT. La SPA lo persiste (TokenService) y lo envia en
 * cada peticion mediante el jwtInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly tokenService = inject(TokenService);
  private readonly router = inject(Router);

  private readonly backendBaseUrl = 'http://localhost:8080';

  /** Usuario actual (null mientras no se ha cargado). */
  private readonly usuarioSignal = signal<Usuario | null>(null);
  readonly usuario = this.usuarioSignal.asReadonly();

  /** Se considera autenticado si hay un JWT persistido. */
  readonly isAuthenticated = computed(() => this.tokenService.hasToken());

  /** Roles del usuario actual. */
  readonly roles = computed(() => this.usuarioSignal()?.roles ?? []);

  readonly perfilCompleto = computed(() => this.esPerfilCompleto(this.usuarioSignal()));

  constructor() {
    // Al arrancar la app (o tras un F5), si hay un JWT persistido recupera el
    // perfil para conservar el estado (nombre/roles) sin re-loguear.
    if (this.tokenService.hasToken()) {
      this.loadCurrentUser().subscribe();
    }
  }

  /** Carga el usuario autenticado desde el backend (GET /me). */
  loadCurrentUser(): Observable<Usuario> {
    return this.api.get<Usuario>('/me').pipe(
      tap({
        next: (usuario) => this.usuarioSignal.set(usuario),
        error: () => this.usuarioSignal.set({ authenticated: false })
      })
    );
  }

  /** Redirige al flujo de autorizacion de Google gestionado por el backend. */
  login(): void {
    window.location.href = `${this.backendBaseUrl}/oauth2/authorization/google`;
  }

  /**
   * Procesa el callback de autenticacion: persiste el JWT emitido por el
   * backend y carga los datos del usuario.
   */
  handleAuthCallback(token: string): void {
    this.tokenService.setToken(token);
  }

  updateCurrentUser(payload: {
    nombre: string;
    foto: string;
    biografia: string;
    redesSociales?: string | null;
    visibilidadPerfil: 'PUBLIC' | 'PRIVATE';
    juegoIds: number[];
  }): Observable<Usuario> {
    return this.api.put<Usuario>('/me', payload).pipe(tap((usuario) => this.usuarioSignal.set(usuario)));
  }

  /** Comprueba si el usuario posee alguno de los roles indicados. */
  hasRole(...roles: string[]): boolean {
    const current = this.roles();
    return roles.some((role) => current.includes(role));
  }

  isProfileComplete(usuario: Usuario | null | undefined = this.usuarioSignal()): boolean {
    return this.esPerfilCompleto(usuario);
  }

  private esPerfilCompleto(usuario: Usuario | null | undefined): boolean {
    if (!usuario?.authenticated) {
      return false;
    }

    return Boolean(
      usuario.nombre?.trim() &&
      usuario.foto?.trim() &&
      usuario.biografia?.trim() &&
      usuario.visibilidadPerfil &&
      (usuario.juegoIds?.length ?? 0) > 0
    );
  }

  /** Cierra la sesion: limpia el token y vuelve al login. */
  logout(): void {
    this.tokenService.clear();
    this.usuarioSignal.set(null);
    this.router.navigate(['/login']);
  }
}
