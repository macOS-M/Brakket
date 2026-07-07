import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

/**
 * Barra superior: marca, accesos rapidos y boton de login/logout.
 */
@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  private readonly authService = inject(AuthService);

  readonly usuario = this.authService.usuario;
  readonly isAuthenticated = this.authService.isAuthenticated;

  login(): void {
    this.authService.login();
  }

  logout(): void {
    this.authService.logout();
  }
}
