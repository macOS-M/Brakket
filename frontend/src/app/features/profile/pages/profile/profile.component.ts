import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, effect, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { ApiService } from '../../../../core/services/api.service';
import { AuthService } from '../../../../core/services/auth.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { FotoInputComponent } from '../../../../shared/components/foto-input/foto-input.component';
import { ElementoProgresion, ProgressionService } from '../../../progression/services/progression.service';
import { FechaInputComponent } from '../../../../shared/components/fecha-input/fecha-input.component';
import { ahoraCostaRica, isoDeFechaLocal } from '../../../../shared/utils/hora-costa-rica';

interface GameOption {
  id: number;
  nombre: string;
}

type SocialKey = 'twitch' | 'x' | 'instagram' | 'youtube';

interface SocialLinkOption {
  key: SocialKey;
  label: string;
  placeholder: string;
}

interface SocialLinksValue {
  twitch: string;
  x: string;
  instagram: string;
  youtube: string;
}

interface ZonaHorariaOption {
  valor: string;
  etiqueta: string;
}

/** Edad mínima para tener cuenta; el backend valida lo mismo (RF-18). */
const EDAD_MINIMA = 13;

/**
 * Mi perfil. Combina la identidad pública que ve la comunidad (nombre visible,
 * avatar, biografía, redes, juegos) con los "ajustes personales" privados
 * (nombre legal, nacimiento, contacto y residencia) del RF-18.
 */
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    PageHeaderComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
    FotoInputComponent,
    FechaInputComponent
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly progressionService = inject(ProgressionService);

  readonly usuario = this.authService.usuario;
  readonly perfilCompleto = this.authService.perfilCompleto;

  readonly socialNetworks: SocialLinkOption[] = [
    { key: 'twitch', label: 'Twitch', placeholder: 'https://twitch.tv/tuusuario' },
    { key: 'x', label: 'X / Twitter', placeholder: 'https://x.com/tuusuario' },
    { key: 'instagram', label: 'Instagram', placeholder: 'https://instagram.com/tuusuario' },
    { key: 'youtube', label: 'YouTube', placeholder: 'https://youtube.com/@tuusuario' }
  ];

  /**
   * Zonas horarias sugeridas: la región del proyecto más las sedes habituales
   * de torneos online. El botón "Detectar" cubre a quien no esté en la lista.
   */
  readonly zonasHorarias: ZonaHorariaOption[] = [
    { valor: 'America/Costa_Rica', etiqueta: 'Costa Rica · GMT-6' },
    { valor: 'America/Guatemala', etiqueta: 'Guatemala · GMT-6' },
    { valor: 'America/El_Salvador', etiqueta: 'El Salvador · GMT-6' },
    { valor: 'America/Tegucigalpa', etiqueta: 'Honduras · GMT-6' },
    { valor: 'America/Managua', etiqueta: 'Nicaragua · GMT-6' },
    { valor: 'America/Panama', etiqueta: 'Panamá · GMT-5' },
    { valor: 'America/Mexico_City', etiqueta: 'México (CDMX) · GMT-6' },
    { valor: 'America/Bogota', etiqueta: 'Colombia · GMT-5' },
    { valor: 'America/Lima', etiqueta: 'Perú · GMT-5' },
    { valor: 'America/Santiago', etiqueta: 'Chile · GMT-4' },
    { valor: 'America/Argentina/Buenos_Aires', etiqueta: 'Argentina · GMT-3' },
    { valor: 'America/Sao_Paulo', etiqueta: 'Brasil (São Paulo) · GMT-3' },
    { valor: 'America/Santo_Domingo', etiqueta: 'Rep. Dominicana · GMT-4' },
    { valor: 'America/New_York', etiqueta: 'EE. UU. Este · GMT-5' },
    { valor: 'America/Chicago', etiqueta: 'EE. UU. Centro · GMT-6' },
    { valor: 'America/Los_Angeles', etiqueta: 'EE. UU. Pacífico · GMT-8' },
    { valor: 'Europe/Madrid', etiqueta: 'España · GMT+1' },
    { valor: 'Europe/London', etiqueta: 'Reino Unido · GMT+0' },
    { valor: 'UTC', etiqueta: 'UTC' }
  ];

  readonly paisesSugeridos = [
    'Costa Rica', 'Guatemala', 'El Salvador', 'Honduras', 'Nicaragua', 'Panamá',
    'México', 'Colombia', 'Perú', 'Chile', 'Argentina', 'Brasil',
    'República Dominicana', 'Estados Unidos', 'España'
  ];

  protected readonly loadingGames = false;
  protected savingProfile = false;
  protected saveError = '';
  protected saveSuccess = '';
  protected games: GameOption[] = [];
  protected tituloAplicado: ElementoProgresion | null = null;
  protected insigniasAplicadas: ElementoProgresion[] = [];

  readonly profileForm = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    foto: ['', [Validators.maxLength(500)]],
    biografia: ['', [Validators.maxLength(2000)]],
    redesSociales: this.fb.nonNullable.group({
      twitch: ['', [Validators.maxLength(500)]],
      x: ['', [Validators.maxLength(500)]],
      instagram: ['', [Validators.maxLength(500)]],
      youtube: ['', [Validators.maxLength(500)]]
    }),
    visibilidadPerfil: ['PUBLIC' as 'PUBLIC' | 'PRIVATE', [Validators.required]],
    juegoIds: this.fb.nonNullable.control<number[]>([]),
    nombreCompleto: ['', [Validators.maxLength(160)]],
    fechaNacimiento: [''],
    telefono: ['', [Validators.maxLength(25)]],
    pais: ['', [Validators.maxLength(80)]],
    ciudad: ['', [Validators.maxLength(120)]],
    direccion: ['', [Validators.maxLength(255)]],
    codigoPostal: ['', [Validators.maxLength(20)]],
    zonaHoraria: ['', [Validators.maxLength(64)]]
  });

  private readonly syncProfileForm = effect(() => {
    const usuario = this.usuario();
    if (!usuario?.authenticated) {
      return;
    }

    this.profileForm.patchValue({
      nombre: usuario.nombre ?? '',
      foto: usuario.foto ?? '',
      biografia: usuario.biografia ?? '',
      redesSociales: this.parseSocialLinks(usuario.redesSociales),
      visibilidadPerfil: usuario.visibilidadPerfil ?? 'PUBLIC',
      juegoIds: usuario.juegoIds ?? [],
      nombreCompleto: usuario.nombreCompleto ?? '',
      fechaNacimiento: usuario.fechaNacimiento ?? '',
      telefono: usuario.telefono ?? '',
      pais: usuario.pais ?? '',
      ciudad: usuario.ciudad ?? '',
      direccion: usuario.direccion ?? '',
      codigoPostal: usuario.codigoPostal ?? '',
      zonaHoraria: usuario.zonaHoraria ?? ''
    }, { emitEvent: false });
  });

  ngOnInit(): void {
    this.api.get<GameOption[]>('/games').subscribe({
      next: (games) => (this.games = games),
      error: () => (this.games = [])
    });
    this.progressionService.get().subscribe({
      next: data => {
        this.tituloAplicado = data.elementos.find(e => e.tipo === 'TITULO' && e.aplicado) ?? null;
        this.insigniasAplicadas = data.elementos.filter(e => e.tipo === 'INSIGNIA' && e.aplicado);
      }
    });
  }

  toggleGame(gameId: number, checked: boolean): void {
    const current = new Set(this.profileForm.controls.juegoIds.value);
    if (checked) {
      current.add(gameId);
    } else {
      current.delete(gameId);
    }
    this.profileForm.controls.juegoIds.setValue([...current]);
    this.profileForm.controls.juegoIds.markAsDirty();
  }

  isGameSelected(gameId: number): boolean {
    return this.profileForm.controls.juegoIds.value.includes(gameId);
  }

  isPrivateVisibility(): boolean {
    return this.profileForm.controls.visibilidadPerfil.value === 'PRIVATE';
  }

  setVisibility(privateProfile: boolean): void {
    this.profileForm.controls.visibilidadPerfil.setValue(privateProfile ? 'PRIVATE' : 'PUBLIC');
    this.profileForm.controls.visibilidadPerfil.markAsDirty();
  }

  /** Tope del datepicker: nadie menor a la edad mínima puede registrarse. */
  get maxFechaNacimiento(): string {
    const hoy = ahoraCostaRica();
    hoy.setFullYear(hoy.getFullYear() - EDAD_MINIMA);
    // Fecha LOCAL, no toISOString(): en GMT-6, después de las 18:00 el
    // día UTC ya es mañana y el tope quedaba corrido un día.
    return isoDeFechaLocal(hoy);
  }

  /** Edad a partir de la fecha cargada; null si no hay fecha o es inválida. */
  get edad(): number | null {
    const valor = this.profileForm.controls.fechaNacimiento.value;
    if (!valor) {
      return null;
    }
    const nacimiento = new Date(`${valor}T00:00:00`);
    if (Number.isNaN(nacimiento.getTime())) {
      return null;
    }
    const hoy = ahoraCostaRica();
    let anios = hoy.getFullYear() - nacimiento.getFullYear();
    const cumplioEsteAnio =
      hoy.getMonth() > nacimiento.getMonth() ||
      (hoy.getMonth() === nacimiento.getMonth() && hoy.getDate() >= nacimiento.getDate());
    if (!cumplioEsteAnio) {
      anios -= 1;
    }
    return anios >= 0 ? anios : null;
  }

  /** Cuántos de los ajustes personales están completos (guía de avance). */
  get camposPersonalesCompletos(): number {
    const c = this.profileForm.controls;
    return [c.nombreCompleto, c.fechaNacimiento, c.telefono, c.pais, c.ciudad, c.direccion,
      c.codigoPostal, c.zonaHoraria]
      .filter((control) => control.value.trim().length > 0).length;
  }

  readonly totalCamposPersonales = 8;

  /** Toma la zona horaria del navegador para no obligar a buscarla en la lista. */
  detectarZonaHoraria(): void {
    const detectada = Intl.DateTimeFormat().resolvedOptions().timeZone;
    if (!detectada) {
      return;
    }
    if (!this.zonasHorarias.some((zona) => zona.valor === detectada)) {
      this.zonasHorarias.push({ valor: detectada, etiqueta: detectada });
    }
    this.profileForm.controls.zonaHoraria.setValue(detectada);
    this.profileForm.controls.zonaHoraria.markAsDirty();
  }

  saveProfile(): void {
    if (this.profileForm.invalid || this.profileForm.pristine || this.savingProfile) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const value = this.profileForm.getRawValue();
    this.savingProfile = true;
    this.saveError = '';
    this.saveSuccess = '';

    this.authService.updateCurrentUser({
      nombre: value.nombre.trim(),
      foto: value.foto.trim() || null,
      biografia: value.biografia.trim() || null,
      redesSociales: this.composeSocialLinks(value.redesSociales),
      visibilidadPerfil: value.visibilidadPerfil,
      juegoIds: value.juegoIds,
      nombreCompleto: value.nombreCompleto.trim() || null,
      fechaNacimiento: value.fechaNacimiento || null,
      telefono: value.telefono.trim() || null,
      pais: value.pais.trim() || null,
      ciudad: value.ciudad.trim() || null,
      direccion: value.direccion.trim() || null,
      codigoPostal: value.codigoPostal.trim() || null,
      zonaHoraria: value.zonaHoraria.trim() || null
    }).pipe(finalize(() => undefined)).subscribe({
      next: () => {
        this.saveSuccess = 'Cambios guardados correctamente.';
        this.profileForm.markAsPristine();
        this.savingProfile = false;
      },
      error: (err: HttpErrorResponse) => {
        this.saveError = this.extraerMensaje(err, 'No se pudieron guardar los cambios.');
        this.savingProfile = false;
      }
    });
  }

  private extraerMensaje(err: HttpErrorResponse, porDefecto: string): string {
    const cuerpo = err.error as { message?: string } | undefined;
    return cuerpo?.message?.trim() || porDefecto;
  }

  private parseSocialLinks(raw: string | undefined | null): SocialLinksValue {
    const emptyValue: SocialLinksValue = {
      twitch: '',
      x: '',
      instagram: '',
      youtube: ''
    };

    if (!raw) {
      return emptyValue;
    }

    const entries = raw
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line.length > 0);

    if (entries.length === 0) {
      return emptyValue;
    }

    const parsed = { ...emptyValue };

    for (const entry of entries) {
      const separatorIndex = entry.indexOf(':');
      if (separatorIndex <= 0) {
        continue;
      }

      const key = entry.slice(0, separatorIndex).trim().toLowerCase();
      const value = entry.slice(separatorIndex + 1).trim();

      if (key === 'twitch') {
        parsed.twitch = value;
      } else if (key === 'x' || key === 'twitter') {
        parsed.x = value;
      } else if (key === 'instagram') {
        parsed.instagram = value;
      } else if (key === 'youtube') {
        parsed.youtube = value;
      }
    }

    return parsed;
  }

  private composeSocialLinks(value: SocialLinksValue): string | null {
    const lines = [
      value.twitch.trim() ? `Twitch: ${value.twitch.trim()}` : '',
      value.x.trim() ? `X: ${value.x.trim()}` : '',
      value.instagram.trim() ? `Instagram: ${value.instagram.trim()}` : '',
      value.youtube.trim() ? `YouTube: ${value.youtube.trim()}` : ''
    ].filter((line) => line.length > 0);

    return lines.length > 0 ? lines.join('\n') : null;
  }
}
