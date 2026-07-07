import { Component, inject } from '@angular/core';

import { AuthService } from '../../core/services/auth.service';

/**
 * EPIC-01: pagina de login. Delega la autenticacion al backend mediante el
 * flujo OAuth2 con Google.
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
