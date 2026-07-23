import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { EquipoElegible, Partida, TorneoDetalle } from '../../../../models/tournament.model';
import { TournamentsService } from '../../services/tournaments.service';
import { AuthService } from '../../../../core/services/auth.service';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import {
  MarcadorEvent,
  TournamentBracketComponent
} from '../../components/tournament-bracket/tournament-bracket.component';
import { portadaFoto, portadaGradiente } from '../../../../shared/utils/cover';
import { FormatoTorneoPipe } from '../../../../shared/pipes/formato-torneo.pipe';

type TabDetalle = 'resumen' | 'llaves' | 'matches' | 'jugadores' | 'resultados';
type FiltroMatch = 'todos' | 'pendientes' | 'finalizadas';

interface JugadorTorneo {
  usuarioId: number;
  nombre: string;
  rol: string;
  equipoId: number;
  equipoNombre: string;
  /** Gamertag del capitán (la identidad del equipo dentro del juego). */
  usuarioEnJuego: string | null;
}

interface EventoTimeline {
  fecha: string | null;
  titulo: string;
  detalle: string;
  cumplido: boolean;
}

/**
 * Detalle de torneo estilo Challenger Mode (RF-24/25/26/27/29): tabs
 * Resumen / Llaves / Matches / Jugadores / Resultados, inscripción con
 * gamertag, inicio del bracket y resultados reporta-confirma.
 */
@Component({
  selector: 'app-tournament-detail',
  standalone: true,
  imports: [RouterLink, DatePipe, EmptyStateComponent, TournamentBracketComponent, FormatoTorneoPipe],
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
  readonly enviandoResultado = signal(false);

  readonly filtroMatch = signal<FiltroMatch>('todos');
  readonly filtroJugador = signal('');

  private readonly torneoId: number;

  readonly torneo = computed(() => this.detalle()?.torneo ?? null);

  /** Tolerante a respuestas del backend previas a V26. */
  readonly ajustes = computed(() => this.torneo()?.ajustesPartida ?? []);

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

  // ---------- Matches ----------

  readonly matches = computed<Partida[]>(() =>
    [...this.partidas()].sort((a, b) => a.ronda - b.ronda || a.orden - b.orden));

  readonly matchesFiltrados = computed<Partida[]>(() => {
    const filtro = this.filtroMatch();
    return this.matches().filter((p) => {
      if (filtro === 'pendientes') {
        return p.estado !== 'FINALIZADA' && p.estado !== 'CANCELADA';
      }
      if (filtro === 'finalizadas') {
        return p.estado === 'FINALIZADA';
      }
      return true;
    });
  });

  // ---------- Jugadores ----------

  readonly jugadores = computed<JugadorTorneo[]>(() =>
    (this.detalle()?.equipos ?? []).flatMap((e) =>
      e.jugadores.map((j) => ({
        usuarioId: j.usuarioId,
        nombre: j.nombre,
        rol: j.rol,
        equipoId: e.equipoId,
        equipoNombre: e.nombre,
        usuarioEnJuego: j.rol === 'CAPITAN' ? e.usuarioEnJuego ?? null : null
      }))));

  readonly jugadoresFiltrados = computed<JugadorTorneo[]>(() => {
    const filtro = this.filtroJugador().trim().toLowerCase();
    if (!filtro) {
      return this.jugadores();
    }
    return this.jugadores().filter((j) =>
      j.nombre.toLowerCase().includes(filtro)
      || j.equipoNombre.toLowerCase().includes(filtro)
      || (j.usuarioEnJuego ?? '').toLowerCase().includes(filtro));
  });

  // ---------- Resultados ----------

  readonly partidasFinalizadas = computed<Partida[]>(() =>
    this.matches().filter((p) => p.estado === 'FINALIZADA' && !p.bye));

  /** Subcampeón: el perdedor de la final (la ronda más alta). */
  readonly subcampeon = computed<string | null>(() => {
    if (!this.finalizado() || this.partidas().length === 0) {
      return null;
    }
    const total = Math.max(...this.partidas().map((p) => p.ronda));
    const final = this.partidas().find((p) => p.ronda === total);
    if (!final || final.ganadorEquipoId === null) {
      return null;
    }
    return final.ganadorEquipoId === final.equipoAId ? final.equipoBNombre : final.equipoANombre;
  });

  /** Línea temporal estilo CM: inscripción → inicio → final. */
  readonly lineaTemporal = computed<EventoTimeline[]>(() => {
    const t = this.torneo();
    if (!t) {
      return [];
    }
    const arranco = this.enCurso() || this.finalizado();
    return [
      {
        fecha: null,
        titulo: 'Inscripción',
        detalle: arranco
          ? 'Registro terminado: ya no podés inscribirte.'
          : this.cupoLleno()
            ? 'Cupo lleno: quedan las inscripciones confirmadas.'
            : 'Los capitanes pueden inscribir a su equipo.',
        cumplido: arranco || this.cupoLleno()
      },
      {
        fecha: t.fechaInicio,
        titulo: 'Comenzar',
        detalle: arranco
          ? 'El torneo ha comenzado: la llave está en juego.'
          : 'El organizador inicia el torneo y se genera la llave.',
        cumplido: arranco
      },
      {
        // La API no expone fechaFin del torneo; el hito no lleva fecha.
        fecha: null,
        titulo: 'Final',
        detalle: this.finalizado()
          ? `Campeón: ${t.campeonNombre ?? '—'}.`
          : 'La final corona al campeón del torneo.',
        cumplido: this.finalizado()
      }
    ];
  });

  constructor() {
    this.torneoId = Number(this.route.snapshot.paramMap.get('id'));
    // Deep-link desde el panel: /tournaments/7?tab=llaves abre esa pestaña.
    const tabInicial = this.route.snapshot.queryParamMap.get('tab');
    if (tabInicial && ['resumen', 'llaves', 'matches', 'jugadores', 'resultados'].includes(tabInicial)) {
      this.tab.set(tabInicial as TabDetalle);
    }
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
        this.tab.set('jugadores');
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
    });
  }

  private recargarDetalle(): void {
    this.tournamentsService.obtener(this.torneoId).subscribe({
      next: (detalle) => this.detalle.set(detalle),
      error: () => undefined
    });
  }

  onMarcador(evento: MarcadorEvent): void {
    this.enviandoResultado.set(true);
    this.errorLlaves.set(null);
    const peticion = evento.resolucion
      ? this.tournamentsService.resolver(evento.partida.id, evento.marcadorA, evento.marcadorB)
      : this.tournamentsService.reportar(evento.partida.id, evento.marcadorA, evento.marcadorB);
    peticion.subscribe({
      next: () => this.refrescarLlaves(),
      error: (err) => {
        this.enviandoResultado.set(false);
        this.errorLlaves.set(err?.error?.message ?? 'No se pudo enviar el resultado.');
      }
    });
  }

  onConfirmar(p: Partida): void {
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

  onRechazar(p: Partida): void {
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

  estadoDePartida(p: Partida): string {
    switch (p.estado) {
      case 'PENDIENTE':
        return p.equipoAId !== null && p.equipoBId !== null ? 'En espera' : 'Por decidir';
      case 'REPORTADA':
        return 'Reportada';
      case 'EN_DISPUTA':
        return 'En disputa';
      case 'FINALIZADA':
        return p.bye ? 'Bye' : 'Finalizada';
      default:
        return p.estado;
    }
  }
}
