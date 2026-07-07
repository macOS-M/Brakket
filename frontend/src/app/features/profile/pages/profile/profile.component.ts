import { Component, inject } from '@angular/core';

import { AuthService } from '../../../../core/services/auth.service';

/**
 * Mi perfil. Muestra los datos del usuario autenticado que devuelve el backend
 * en GET /api/me (nombre, correo, foto y roles).
 */
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent {
  private readonly authService = inject(AuthService);

  readonly usuario = this.authService.usuario;
}
