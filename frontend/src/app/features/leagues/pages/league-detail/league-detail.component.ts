import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { FormatOption, League, Season, SeasonRequest, SeasonStatus } from '../../../../models/league.model';
import { LeaguesService } from '../../services/leagues.service';

function rangoFechasValido(control: AbstractControl): ValidationErrors | null {
  const inicio = control.get('fechaInicio')?.value;
  const fin = control.get('fechaFin')?.value;
  return inicio && fin && inicio > fin ? { rangoFechas: true } : null;
}

@Component({
  selector: 'app-league-detail', standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  templateUrl: './league-detail.component.html', styleUrl: './league-detail.component.scss'
})
export class LeagueDetailComponent {
  private readonly fb = inject(FormBuilder);
  private readonly leaguesService = inject(LeaguesService);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  readonly league = signal<League | null>(null);
  readonly seasons = signal<Season[]>([]);
  readonly formats = signal<FormatOption[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly savingSeason = signal(false);
  readonly seasonError = signal<string | null>(null);
  readonly confirmation = signal<string | null>(null);
  readonly editingId = signal<number | null>(null);
  readonly showingForm = signal(false);
  private pointerInicioEnFondo = false;
  readonly esComisionado = computed(() => {
    const liga = this.league(); const usuario = this.auth.usuario();
    return !!liga && !!usuario?.id && Number(usuario.id) === liga.comisionadoId;
  });
  private readonly ligaId = Number(this.route.snapshot.paramMap.get('id'));
  readonly seasonForm = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    fechaInicio: ['', Validators.required], fechaFin: ['', Validators.required],
    reglas: ['', [Validators.required, Validators.maxLength(5000)]],
    estado: ['PLANIFICADA' as SeasonStatus, Validators.required],
    cupoEquipos: [8, [Validators.required, Validators.min(2), Validators.max(1024)]],
    formatoId: [null as number | null, Validators.required]
  }, { validators: rangoFechasValido });

  constructor() {
    this.leaguesService.getById(this.ligaId).subscribe({
      next: liga => { this.league.set(liga); this.loading.set(false); },
      error: () => { this.error.set('No se pudo cargar la liga.'); this.loading.set(false); }
    });
    this.cargarTemporadas();
    this.leaguesService.seasonFormats(this.ligaId).subscribe({
      next: items => this.formats.set(items),
      error: () => this.seasonError.set('No se pudieron cargar los formatos activos del juego.')
    });
  }

  private cargarTemporadas(): void {
    this.leaguesService.listSeasons(this.ligaId).subscribe({
      next: items => this.seasons.set(items),
      error: () => this.seasonError.set('No se pudieron cargar las temporadas.')
    });
  }

  editar(temporada: Season): void {
    this.editingId.set(temporada.id); this.confirmation.set(null); this.seasonError.set(null);
    this.showingForm.set(true);
    this.seasonForm.setValue({ nombre: temporada.nombre, fechaInicio: temporada.fechaInicio,
      fechaFin: temporada.fechaFin, reglas: temporada.reglas, estado: temporada.estado,
      cupoEquipos: temporada.cupoEquipos, formatoId: temporada.formatoId });
  }

  abrirCreacion(): void {
    this.editingId.set(null); this.confirmation.set(null); this.seasonError.set(null);
    this.resetForm(); this.showingForm.set(true);
  }

  cancelarEdicion(): void { this.editingId.set(null); this.showingForm.set(false); this.resetForm(); }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    if (this.showingForm() && !this.savingSeason()) this.cancelarEdicion();
  }

  registrarInicioEnFondo(event: PointerEvent): void {
    this.pointerInicioEnFondo = event.target === event.currentTarget;
  }

  cerrarDesdeFondo(event: PointerEvent): void {
    const terminoEnFondo = event.target === event.currentTarget;
    if (this.pointerInicioEnFondo && terminoEnFondo && !this.savingSeason()) this.cancelarEdicion();
    this.pointerInicioEnFondo = false;
  }

  guardarTemporada(): void {
    if (this.seasonForm.invalid) { this.seasonForm.markAllAsTouched(); return; }
    const v = this.seasonForm.getRawValue();
    const body: SeasonRequest = { nombre: v.nombre!.trim(), fechaInicio: v.fechaInicio!, fechaFin: v.fechaFin!,
      reglas: v.reglas!.trim(), estado: v.estado!, cupoEquipos: Number(v.cupoEquipos), formatoId: Number(v.formatoId) };
    this.savingSeason.set(true); this.seasonError.set(null); this.confirmation.set(null);
    const id = this.editingId();
    const request = id == null
      ? this.leaguesService.createSeason(this.ligaId, body)
      : this.leaguesService.updateSeason(this.ligaId, id, { configuracion: body,
          version: this.seasons().find(s => s.id === id)!.version });
    request.subscribe({
      next: temporada => {
        this.seasons.update(items => id == null ? [...items, temporada].sort((a,b) => a.fechaInicio.localeCompare(b.fechaInicio))
          : items.map(item => item.id === id ? temporada : item));
        this.confirmation.set(temporada.mensaje ?? (id == null ? 'Temporada creada correctamente.' : 'Temporada actualizada correctamente.'));
        this.editingId.set(null); this.showingForm.set(false); this.resetForm(); this.savingSeason.set(false);
      },
      error: err => { this.seasonError.set(err?.error?.message ?? 'No se pudo guardar la temporada.'); this.savingSeason.set(false); }
    });
  }

  private resetForm(): void {
    this.seasonForm.reset({ estado: 'PLANIFICADA', cupoEquipos: 8, formatoId: null, nombre: '', fechaInicio: '', fechaFin: '', reglas: '' });
  }
}
