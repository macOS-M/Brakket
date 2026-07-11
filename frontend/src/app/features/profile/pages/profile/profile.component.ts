import { Component, OnInit, effect, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { ApiService } from '../../../../core/services/api.service';
import { AuthService } from '../../../../core/services/auth.service';

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

/**
 * Mi perfil. Muestra los datos del usuario autenticado que devuelve el backend
 * en GET /api/me (nombre, correo, foto y roles).
 */
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly api = inject(ApiService);

  readonly usuario = this.authService.usuario;
  readonly perfilCompleto = this.authService.perfilCompleto;

  readonly socialNetworks: SocialLinkOption[] = [
    { key: 'twitch', label: 'Twitch', placeholder: 'https://twitch.tv/tuusuario' },
    { key: 'x', label: 'X / Twitter', placeholder: 'https://x.com/tuusuario' },
    { key: 'instagram', label: 'Instagram', placeholder: 'https://instagram.com/tuusuario' },
    { key: 'youtube', label: 'YouTube', placeholder: 'https://youtube.com/@tuusuario' }
  ];

  protected readonly loadingGames = false;
  protected savingProfile = false;
  protected saveError = '';
  protected saveSuccess = '';
  protected games: GameOption[] = [];

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
    juegoIds: this.fb.nonNullable.control<number[]>([])
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
      juegoIds: usuario.juegoIds ?? []
    }, { emitEvent: false });
  });

  ngOnInit(): void {
    this.api.get<GameOption[]>('/games').subscribe({
      next: (games) => (this.games = games),
      error: () => (this.games = [])
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
      juegoIds: value.juegoIds
    }).pipe(finalize(() => undefined)).subscribe({
      next: () => {
        this.saveSuccess = 'Cambios guardados correctamente.';
        this.profileForm.markAsPristine();
        this.savingProfile = false;
      },
      error: () => {
        this.saveError = 'No se pudieron guardar los cambios.';
        this.savingProfile = false;
      }
    });
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
