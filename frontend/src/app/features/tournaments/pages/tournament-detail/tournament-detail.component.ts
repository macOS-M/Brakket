import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { EquipoElegible, Partida, TorneoDetalle } from '../../../../models/tournament.model';
import { TournamentsService } from '../../services/tournaments.service';
import { AuthService } from '../../../../core/services/auth.service';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { portadaFoto, portadaGradiente } from '../../../../shared/utils/cover';

type TabDetalle = 'resumen' | 'equipos' | 'llaves';

interface Ronda {
  numero: number;
  etiqueta: string;
  partidas: Partida[];
}

/**
 * Detalle de torneo (RF-24/RF-25) + torneo en vivo (RF-26/27/29): banner,
 * tabs Resumen / Equipos / Llaves, inscripción con gamertag, inicio del
 * bracket por el organizador y resultados reporta-confirma con lobby.
 */
@Component({
  selector: 'app-tournament-detail',
  standalone: true,
  imports: [RouterLink, DatePipe, EmptyStateComponent],
  templateUrl: './tournament-detail.component.html',
  styleUrl: './tournament-detail.component.scss'
})
export class TournamentDetailComponent {
  private readonly tournamentsService = inject(TournamentsService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  readonly detalle = signal<TorneoDetalle | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly tab = signal<TabDetalle>('resumen');

  readonly elegibles = signal<EquipoElegible[]>([]);
  readonly equipoElegido = signal<number | null>(null);
  readonly usuarioEnJuego = signal('');
  readonly inscribiendo = signal(false);
  readonly errorInscripcion = signal<string | null>(null);
  readonly inscripcionExitosa = signal(false);

  readonly confirmandoEliminar = signal(false);
  readonly eliminando = signal(false);
  readonly errorEliminar = signal<string | null>(null);

  // ---------- torneo en vivo ----------
  readonly partidas = signal<Partida[]>([]);
  readonly iniciando = signal(false);
  readonly errorLlaves = signal<string | null>(null);
  /** Partida con el formulario de marcador abierto (reporte o resolución). */
  readonly reportando = signal<number | null>(null);
  readonly marcadorA = signal(0);
  readonly marcadorB = signal(0);
  readonly enviandoResultado = signal(false);

  private readonly torneoId: number;

  readonly torneo = computed(() => this.detalle()?.torneo ?? null);

  readonly arte = computed(() => {
    const t = this.torneo();
    return t ? t.juegoImagenUrl || portadaFoto(t.juegoNombre) : null;
  });

  readonly gradiente = computed(() => portadaGradiente(this.torneo()?.juegoNombre ?? '?'));

  readonly cupoLleno = computed(() => {
    const t = this.torneo();
    return !!t && t.inscritos >= t.maxEquipos;
  });

  readonly comenzo = computed(() => {
    const t = this.torneo();
    return !!t && new Date(t.fechaInicio) <= new Date();
  });

  readonly abierto = computed(() => {
    const t = this.torneo();
    return !!t && t.estado === 'INSCRIPCION_ABIERTA' && !this.comenzo() && !this.cupoLleno();
  });

  readonly enCurso = computed(() => this.torneo()?.estado === 'EN_CURSO');
  readonly finalizado = computed(() => this.torneo()?.estado === 'FINALIZADO');

  readonly esOrganizador = computed(() => {
    const t = this.torneo();
    const usuario = this.auth.usuario();
    return !!t && !!usuario?.id && Number(usuario.id) === t.organizadorId;
  });

  readonly puedeEliminar = computed(
    () => this.esOrganizador() || this.auth.hasRole('ADMIN')
  );

  /** La ve un ADMIN sobre un torneo ajeno: es moderación, no gestión propia. */
  readonly esModeracion = computed(() => this.puedeEliminar() && !this.esOrganizador());

  readonly esGestor = computed(() => this.esOrganizador() || this.auth.hasRole('ADMIN'));

  /** Iniciar exige gestor, etapa de inscripción y al menos 2 equipos. */
  readonly puedeIniciar = computed(() => {
    const t = this.torneo();
    return !!t && this.esGestor() && t.estado === 'INSCRIPCION_ABIERTA' && t.inscritos >= 2;
  });

  /** Equipos de este torneo cuyo capitán es el usuario actual. */
  readonly misEquipos = computed<number[]>(() => {
    const uid = Number(this.auth.usuario()?.id);
    if (!uid) {
      return [];
    }
    return (this.detalle()?.equipos ?? [])
      .filter((e) => e.jugadores.some((j) => j.usuarioId === uid && j.rol === 'CAPITAN'))
      .map((e) => e.equipoId);
  });

  /** El bracket agrupado por ronda, con etiquetas humanas (Final, Semis…). */
  readonly rondas = computed<Ronda[]>(() => {
    const lista = this.partidas();
    if (lista.length === 0) {
      return [];
    }
    const total = Math.max(...lista.map((p) => p.ronda));
    const porRonda = new Map<number, Partida[]>();
    for (const p of lista) {
      porRonda.set(p.ronda, [...(porRonda.get(p.ronda) ?? []), p]);
    }
    return [...porRonda.entries()]
      .sort(([a], [b]) => a - b)
      .map(([numero, partidas]) => ({
        numero,
        etiqueta: this.etiquetaRonda(numero, total),
        partidas: partidas.sort((a, b) => a.orden - b.orden)
      }));
  });

  constructor() {
    this.torneoId = Number(this.route.snapshot.paramMap.get('id'));
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.tournamentsService.obtener(this.torneoId).subscribe({
      next: (detalle) => {
        this.detalle.set(detalle);
        this.cargando.set(false);
        this.cargarElegibles();
        const estado = detalle.torneo.estado;
        if (estado === 'EN_CURSO' || estado === 'FINALIZADO') {
          this.cargarBracket();
        }
      },
      error: () => {
        this.error.set('No se pudo cargar el torneo (¿existe o es privado?).');
        this.cargando.set(false);
      }
    });
  }

  private cargarElegibles(): void {
    if (!this.auth.isAuthenticated() || !this.abierto()) {
      return;
    }
    this.tournamentsService.equiposElegibles(this.torneoId).subscribe({
      next: (equipos) => {
        this.elegibles.set(equipos);
        if (equipos.length === 1) {
          this.equipoElegido.set(equipos[0].id);
        }
      },
      error: () => this.elegibles.set([])
    });
  }

  inscribir(): void {
    const equipoId = this.equipoElegido();
    const gamertag = this.usuarioEnJuego().trim();
    if (!equipoId || !gamertag) {
      return;
    }
    this.inscribiendo.set(true);
    this.errorInscripcion.set(null);
    this.tournamentsService.inscribir(this.torneoId, equipoId, gamertag).subscribe({
      next: (detalle) => {
        this.detalle.set(detalle);
        this.inscribiendo.set(false);
        this.inscripcionExitosa.set(true);
        this.elegibles.update((lista) => lista.filter((e) => e.id !== equipoId));
        this.equipoElegido.set(null);
        this.usuarioEnJuego.set('');
        this.tab.set('equipos');
      },
      error: (err) => {
        this.inscribiendo.set(false);
        this.errorInscripcion.set(err?.error?.message ?? 'No se pudo inscribir el equipo.');
      }
    });
  }

  eliminar(): void {
    this.eliminando.set(true);
    this.errorEliminar.set(null);
    this.tournamentsService.eliminar(this.torneoId).subscribe({
      next: () => this.router.navigate(['/tournaments']),
      error: (err) => {
        this.eliminando.set(false);
        this.confirmandoEliminar.set(false);
        this.errorEliminar.set(err?.error?.message ?? 'No se pudo eliminar el torneo.');
      }
    });
  }

  // ---------- torneo en vivo ----------

  iniciarTorneo(): void {
    this.iniciando.set(true);
    this.errorLlaves.set(null);
    this.tournamentsService.iniciar(this.torneoId).subscribe({
      next: (partidas) => {
        this.partidas.set(partidas);
        this.iniciando.set(false);
        this.tab.set('llaves');
        this.recargarDetalle();
      },
      error: (err) => {
        this.iniciando.set(false);
        this.errorLlaves.set(err?.error?.message ?? 'No se pudo iniciar el torneo.');
      }
    });
  }

  private cargarBracket(): void {
    this.tournamentsService.bracket(this.torneoId).subscribe({
      next: (partidas) => this.partidas.set(partidas),
      error: () => this.partidas.set([])
    });
  }

  /** Tras cada resultado el avance puede tocar otras partidas y el torneo. */
  private refrescarLlaves(): void {
    forkJoin({
      detalle: this.tournamentsService.obtener(this.torneoId).pipe(catchError(() => of(null))),
      partidas: this.tournamentsService.bracket(this.torneoId).pipe(catchError(() => of([] as Partida[])))
    }).subscribe(({ detalle, partidas }) => {
      if (detalle) {
        this.detalle.set(detalle);
      }
      this.partidas.set(partidas);
      this.enviandoResultado.set(false);
      this.reportando.set(null);
    });
  }

  private recargarDetalle(): void {
    this.tournamentsService.obtener(this.torneoId).subscribe({
      next: (detalle) => this.detalle.set(detalle),
      error: () => undefined
    });
  }

  soyCapitanDe(equipoId: number | null): boolean {
    return equipoId !== null && this.misEquipos().includes(equipoId);
  }

  /** Capitán de cualquiera de los dos equipos, con la partida lista. */
  puedeReportar(p: Partida): boolean {
    return this.enCurso() && p.estado === 'PENDIENTE'
      && (this.soyCapitanDe(p.equipoAId) || this.soyCapitanDe(p.equipoBId));
  }

  /** Confirmar/rechazar es del capitán del equipo que NO reportó. */
  puedeConfirmar(p: Partida): boolean {
    if (!this.enCurso() || p.estado !== 'REPORTADA' || p.reportadoPorEquipoId === null) {
      return false;
    }
    const rival = p.reportadoPorEquipoId === p.equipoAId ? p.equipoBId : p.equipoAId;
    return this.soyCapitanDe(rival);
  }

  /** El gestor destraba cualquier partida activa (rival ausente, disputa…). */
  puedeResolver(p: Partida): boolean {
    return this.enCurso() && this.esGestor() && !p.bye
      && p.equipoAId !== null && p.equipoBId !== null
      && (p.estado === 'PENDIENTE' || p.estado === 'REPORTADA' || p.estado === 'EN_DISPUTA');
  }

  abrirMarcador(p: Partida): void {
    this.reportando.set(p.id);
    this.marcadorA.set(p.marcadorA ?? 0);
    this.marcadorB.set(p.marcadorB ?? 0);
    this.errorLlaves.set(null);
  }

  enviarReporte(p: Partida, comoResolucion: boolean): void {
    if (this.marcadorA() === this.marcadorB()) {
      this.errorLlaves.set('En eliminación directa no hay empates: los marcadores deben diferir.');
      return;
    }
    this.enviandoResultado.set(true);
    this.errorLlaves.set(null);
    const peticion = comoResolucion
      ? this.tournamentsService.resolver(p.id, this.marcadorA(), this.marcadorB())
      : this.tournamentsService.reportar(p.id, this.marcadorA(), this.marcadorB());
    peticion.subscribe({
      next: () => this.refrescarLlaves(),
      error: (err) => {
        this.enviandoResultado.set(false);
        this.errorLlaves.set(err?.error?.message ?? 'No se pudo enviar el resultado.');
      }
    });
  }

  confirmarResultado(p: Partida): void {
    this.enviandoResultado.set(true);
    this.errorLlaves.set(null);
    this.tournamentsService.confirmar(p.id).subscribe({
      next: () => this.refrescarLlaves(),
      error: (err) => {
        this.enviandoResultado.set(false);
        this.errorLlaves.set(err?.error?.message ?? 'No se pudo confirmar el resultado.');
      }
    });
  }

  rechazarResultado(p: Partida): void {
    this.enviandoResultado.set(true);
    this.errorLlaves.set(null);
    this.tournamentsService.rechazar(p.id).subscribe({
      next: () => this.refrescarLlaves(),
      error: (err) => {
        this.enviandoResultado.set(false);
        this.errorLlaves.set(err?.error?.message ?? 'No se pudo rechazar el resultado.');
      }
    });
  }

  private etiquetaRonda(numero: number, total: number): string {
    const desdeElFinal = total - numero;
    if (desdeElFinal === 0) {
      return 'Final';
    }
    if (desdeElFinal === 1) {
      return 'Semifinales';
    }
    if (desdeElFinal === 2) {
      return 'Cuartos de final';
    }
    if (desdeElFinal === 3) {
      return 'Octavos de final';
    }
    return `Ronda ${numero}`;
  }
}
