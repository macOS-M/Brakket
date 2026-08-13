import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { GameOption, League, LeagueRequest } from '../../../../models/league.model';
import { LeaguesService } from '../../services/leagues.service';
import { FotoInputComponent } from '../../../../shared/components/foto-input/foto-input.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';

/**
 * Formulario para crear una liga o configurar (editar) una existente (RF-22).
 * Modo edición cuando la ruta incluye un :id; modo creación en caso contrario.
 */
@Component({
  selector: 'app-league-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, FotoInputComponent, PageHeaderComponent],
  templateUrl: './league-form.component.html',
  styleUrl: './league-form.component.scss'
})
export class LeagueFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly leaguesService = inject(LeaguesService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly games = signal<GameOption[]>([]);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly leagueId = signal<number | null>(null);
  private readonly ligaCargada = signal<League | null>(null);

  /** En modo edición, si el usuario actual no es el comisionado se bloquea el form. */
  readonly sinPermiso = computed(() => {
    const liga = this.ligaCargada();
    const usuario = this.auth.usuario();
    return !!liga && !!usuario?.id && Number(usuario.id) !== liga.comisionadoId;
  });

  readonly form = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    juegoId: [null as number | null, [Validators.required]],
    descripcion: ['', [Validators.maxLength(1000)]],
    reglas: ['', [Validators.maxLength(4000)]],
    fotoUrl: ['', [Validators.maxLength(500)]]
  });

  constructor() {
    this.leaguesService.gameOptions().subscribe({
      next: (juegos) => this.games.set(juegos),
      error: () => this.error.set('No se pudieron cargar los juegos disponibles.')
    });

    // Venir desde el hub de un juego preselecciona ese juego (?juegoId=).
    const juegoParam = this.route.snapshot.queryParamMap.get('juegoId');
    if (juegoParam) {
      this.form.patchValue({ juegoId: Number(juegoParam) });
    }

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.leagueId.set(id);
      this.leaguesService.getById(id).subscribe({
        next: (liga: League) => {
          this.ligaCargada.set(liga);
          this.form.patchValue({
            nombre: liga.nombre,
            juegoId: liga.juegoId,
            descripcion: liga.descripcion ?? '',
            reglas: liga.reglas ?? '',
            fotoUrl: liga.fotoUrl ?? ''
          });
        },
        error: () => this.error.set('No se pudo cargar la liga.')
      });
    }
  }

  get esEdicion(): boolean {
    return this.leagueId() !== null;
  }

  /**
   * Arte del juego, que es la portada de reserva mientras la liga no tenga
   * foto propia. Con foto propia devuelve null: la vista previa de esa la
   * pinta `app-foto-input`.
   */
  get artePorDefecto(): string | null {
    if (this.form.value.fotoUrl?.trim()) {
      return null;
    }
    return this.ligaCargada()?.juegoImagenUrl ?? null;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);

    const body: LeagueRequest = {
      nombre: this.form.value.nombre!.trim(),
      juegoId: Number(this.form.value.juegoId),
      descripcion: this.form.value.descripcion?.trim() || null,
      reglas: this.form.value.reglas?.trim() || null,
      fotoUrl: this.form.value.fotoUrl?.trim() || null
    };

    const id = this.leagueId();
    const peticion = id
      ? this.leaguesService.update(id, body)
      : this.leaguesService.create(body);

    peticion.subscribe({
      next: (liga) => this.router.navigate(['/leagues', liga.id]),
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo guardar la liga.');
        this.saving.set(false);
      }
    });
  }
}
