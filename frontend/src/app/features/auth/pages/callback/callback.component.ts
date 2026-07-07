import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';

/**
 * Recibe el redirect del backend tras el login con Google
 * (GET /auth/callback?token=...). Persiste el JWT, carga el usuario actual
 * y navega al inicio. Si no llega token, vuelve al login.
 */
@Component({
  selector: 'app-auth-callback',
  standalone: true,
  imports: [],
  templateUrl: './callback.component.html',
  styleUrl: './callback.component.scss'
})
export class CallbackComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.router.navigate(['/login']);
      return;
    }

    this.authService.handleAuthCallback(token);
    this.router.navigate(['/']);
  }
}
