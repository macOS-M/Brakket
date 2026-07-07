import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

/**
 * Estructura principal de la aplicacion: barra de navegacion superior con
 * enlaces a las features y un router-outlet donde se renderizan las paginas.
 */
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss'
})
export class LayoutComponent {
  private readonly authService = inject(AuthService);

  readonly usuario = this.authService.usuario;
  readonly isAuthenticated = this.authService.isAuthenticated;

  readonly enlaces = [
    { ruta: '/equipos', etiqueta: 'Equipos' },
    { ruta: '/ligas', etiqueta: 'Ligas' },
    { ruta: '/torneos', etiqueta: 'Torneos' },
    { ruta: '/disputas', etiqueta: 'Disputas' },
    { ruta: '/twitch', etiqueta: 'Twitch' },
    { ruta: '/patrocinios', etiqueta: 'Patrocinios' },
    { ruta: '/estadisticas', etiqueta: 'Estadisticas' },
    { ruta: '/admin', etiqueta: 'Admin' }
  ];

  login(): void {
    this.authService.login();
  }

  logout(): void {
    this.authService.logout();
  }
}
