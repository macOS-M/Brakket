import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';

type ModoLocal = 'login' | 'registro';

/**
 * Pagina de inicio de sesion: Google (flujo OAuth2 del backend) o cuenta
 * local con correo y contraseña (DD-04, referencia Challenger Mode). Ambos
 * caminos terminan en el mismo JWT.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly loginError = computed(() => this.route.snapshot.queryParamMap.get('error'));
  readonly oauthError = computed(() => this.route.snapshot.queryParamMap.get('oauth_error'));
  readonly errorMessage = computed(() => this.route.snapshot.queryParamMap.get('error_message'));
  readonly oauthErrorDescription = computed(() =>
    this.route.snapshot.queryParamMap.get('oauth_error_description')
  );

  readonly modo = signal<ModoLocal>('login');
  readonly enviando = signal(false);
  readonly errorLocal = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    nombre: [''],
    correo: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  login(): void {
    this.authService.login();
  }

  cambiarModo(modo: ModoLocal): void {
    this.modo.set(modo);
    this.errorLocal.set(null);
    // El nombre solo es obligatorio al registrarse.
    const nombre = this.form.controls.nombre;
    if (modo === 'registro') {
      nombre.addValidators([Validators.required, Validators.maxLength(120)]);
    } else {
      nombre.clearValidators();
    }
    nombre.updateValueAndValidity();
  }

  enviarLocal(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.errorLocal.set(null);

    const { nombre, correo, password } = this.form.getRawValue();
    const peticion =
      this.modo() === 'registro'
        ? this.authService.registroLocal(nombre.trim(), correo.trim(), password)
        : this.authService.loginLocal(correo.trim(), password);

    peticion.subscribe({
      next: (usuario) => this.router.navigateByUrl(this.authService.rutaPostLogin(usuario)),
      error: (err) => {
        this.enviando.set(false);
        this.errorLocal.set(
          err?.error?.message ??
            (this.modo() === 'registro'
              ? 'No se pudo crear la cuenta.'
              : 'No se pudo iniciar sesión.')
        );
      }
    });
  }
}
