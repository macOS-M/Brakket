import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
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

  // Mostrar/ocultar el texto de cada campo de contraseña, cada uno por separado.
  readonly verPassword = signal(false);
  readonly verConfirmarPassword = signal(false);

  /** Solo exige coincidencia en modo registro; en login no hay confirmarPassword que comparar. */
  private readonly confirmarPasswordValidator: ValidatorFn = (control) => {
    const grupo = control.parent;
    if (!grupo || this.modo() !== 'registro') {
      return null;
    }
    return grupo.get('password')?.value === control.value ? null : { passwordsNoCoinciden: true };
  };

  readonly form = this.fb.nonNullable.group({
    nombre: [''],
    correo: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmarPassword: ['', [this.confirmarPasswordValidator]]
  });

  constructor() {
    // Si cambia la contraseña, hay que revisar de nuevo si sigue coincidiendo
    // con lo que ya se había escrito en "confirmar" (si no, el error se
    // queda pegado aunque el usuario ya haya corregido la contraseña).
    this.form.controls.password.valueChanges.subscribe(() =>
      this.form.controls.confirmarPassword.updateValueAndValidity({ onlySelf: true, emitEvent: false })
    );
  }

  login(): void {
    this.authService.login();
  }

  toggleVerPassword(): void {
    this.verPassword.update((v) => !v);
  }

  toggleVerConfirmarPassword(): void {
    this.verConfirmarPassword.update((v) => !v);
  }

  cambiarModo(modo: ModoLocal): void {
    this.modo.set(modo);
    this.errorLocal.set(null);
    // El nombre y la confirmación de contraseña solo son obligatorios al registrarse.
    const nombre = this.form.controls.nombre;
    const confirmarPassword = this.form.controls.confirmarPassword;
    if (modo === 'registro') {
      nombre.addValidators([Validators.required, Validators.maxLength(120)]);
      confirmarPassword.addValidators([Validators.required]);
    } else {
      nombre.clearValidators();
      confirmarPassword.clearValidators();
    }
    nombre.updateValueAndValidity();
    confirmarPassword.updateValueAndValidity();
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
