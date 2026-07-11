import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

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
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  readonly loginError = computed(() => this.route.snapshot.queryParamMap.get('error'));
  readonly oauthError = computed(() => this.route.snapshot.queryParamMap.get('oauth_error'));
  readonly errorMessage = computed(() => this.route.snapshot.queryParamMap.get('error_message'));
  readonly oauthErrorDescription = computed(() =>
    this.route.snapshot.queryParamMap.get('oauth_error_description')
  );

  login(): void {
    this.authService.login();
  }
}
