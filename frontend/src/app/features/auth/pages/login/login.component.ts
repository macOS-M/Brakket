import { Component, inject } from '@angular/core';

import { AuthService } from '../../../../core/services/auth.service';

/**
 * Pagina de inicio de sesion. Delega la autenticacion al backend mediante el
 * flujo OAuth2 con Google; el backend devuelve un JWT que la SPA persiste.
 * Pendiente EPIC-01.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly authService = inject(AuthService);

  login(): void {
    this.authService.login();
  }
}
