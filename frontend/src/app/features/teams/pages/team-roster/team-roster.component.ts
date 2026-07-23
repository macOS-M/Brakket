import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, forkJoin, of, takeUntil } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { Equipo } from '../../../../models/equipo.model';
import { MiembroEquipo, ROLES_EQUIPO } from '../../../../models/miembro-equipo.model';
import { JugadorDisponible } from '../../../../models/jugador-disponible.model';
import { Juego } from '../../../../models/juego.model';
import { SolicitudUnion } from '../../../../models/solicitud-union.model';
import { TeamsService } from '../../services/teams.service';
import { GamesService } from '../../../games/services/games.service';
import { AuthService } from '../../../../core/services/auth.service';

/** Jugador elegido para invitar, con el rol que le propone el capitán. */
interface SeleccionInvitacion {
  id: number;
  nombre: string;
  rol: string;
}

/** Orden de presentación de la plantilla (RF-08): capitán primero. */
const ORDEN_ROLES: Record<string, number> = { CAPITAN: 0, TITULAR: 1, SUPLENTE: 2, COACH: 3 };

@Component({
  selector: 'app-team-roster',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './team-roster.component.html',
  styleUrl: './team-roster.component.scss'
})
export class TeamRosterComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly teamsService = inject(TeamsService);
  private readonly gamesService = inject(GamesService);
  private readonly authService = inject(AuthService);
  private readonly destroy$ = new Subject<void>();
  /** La búsqueda corre sola mientras se escribe (con debounce). */
  private readonly busquedaEnVivo$ = new Subject<void>();

  readonly roles = ROLES_EQUIPO;
  readonly miembros = signal<MiembroEquipo[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly guardandoId = signal<number | null>(null);
  readonly esCapitan = computed(() => {
    const miId = Number(this.authService.usuario()?.id);
    return this.miembros().some(
      (m) => m.usuarioId === miId && m.rol === 'CAPITAN' && m.estado === 'ACTIVO'
    );
  });

  // RF-08: datos del equipo para encabezado y vista histórica si está disuelto.
  readonly equipo = signal<Equipo | null>(null);

  /** Plantilla ordenada: activos primero (capitán, titulares, suplentes, coach), luego el resto. */
  readonly miembrosOrdenados = computed(() =>
    [...this.miembros()].sort((a, b) => {
      const activoA = a.estado === 'ACTIVO' ? 0 : 1;
      const activoB = b.estado === 'ACTIVO' ? 0 : 1;
      if (activoA !== activoB) return activoA - activoB;
      const rolA = ORDEN_ROLES[a.rol] ?? 9;
      const rolB = ORDEN_ROLES[b.rol] ?? 9;
      if (rolA !== rolB) return rolA - rolB;
      return a.nombreUsuario.localeCompare(b.nombreUsuario);
    })
  );

  /** El equipo está disuelto: la plantilla se muestra como vista histórica de solo lectura. */
  readonly vistaHistorica = computed(
    () => this.equipoDisuelto() !== null || this.equipo()?.estado === 'DISUELTO'
  );

  readonly invitando = signal(false);
  readonly errorInvitar = signal<string | null>(null);
  readonly invitacionEnviada = signal(false);

  // RF-10: expulsión de integrantes (solo el capitán; el backend lo valida).
  readonly miembroAExpulsar = signal<MiembroEquipo | null>(null);
  readonly causaExpulsion = signal('');
  readonly expulsando = signal(false);
  readonly errorExpulsion = signal<string | null>(null);

  // RF-03: disolución del equipo (solo el capitán; el backend lo valida).
  readonly confirmaDisolucion = signal(false);
  readonly motivoDisolucion = signal('');
  readonly disolviendo = signal(false);
  readonly errorDisolucion = signal<string | null>(null);
  readonly equipoDisuelto = signal<Equipo | null>(null);

  // RF-11: buscar jugadores disponibles.
  readonly juegos = signal<Juego[]>([]);
  readonly textoBusqueda = signal('');
  readonly juegoIdBusqueda = signal<number | null>(null);
  readonly soloDisponibles = signal(false);
  readonly buscando = signal(false);
  readonly errorBusqueda = signal<string | null>(null);
  readonly resultados = signal<JugadorDisponible[] | null>(null);

  private equipoId!: number;

  /** Selección múltiple para invitar de una vez, cada uno con su rol. */
  readonly seleccionados = signal<SeleccionInvitacion[]>([]);
  readonly mensajeInvitacion = signal('');
  readonly resumenInvitaciones = signal<string | null>(null);

  // Solicitudes de unión pendientes (las ve y responde el capitán).
  readonly solicitudes = signal<SolicitudUnion[]>([]);
  readonly respondiendoSolicitud = signal<number | null>(null);
  readonly errorSolicitudes = signal<string | null>(null);

  ngOnInit(): void {
    this.equipoId = Number(this.route.snapshot.paramMap.get('equipoId'));
    this.cargarEquipo();
    this.cargarMiembros();
    this.gamesService.listActivos().subscribe({
      next: (juegos) => this.juegos.set(juegos)
    });
    this.busquedaEnVivo$
      .pipe(debounceTime(300), takeUntil(this.destroy$))
      .subscribe(() => this.buscarJugadores());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** RF-08: carga nombre y estado del equipo; si falla, la plantilla sigue siendo usable. */
  private cargarEquipo(): void {
    this.teamsService.obtenerPorId(this.equipoId).subscribe({
      next: (equipo) => this.equipo.set(equipo),
      error: () => this.equipo.set(null)
    });
  }

  cargarMiembros(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.teamsService.listMiembros(this.equipoId).subscribe({
      next: (miembros) => {
        this.miembros.set(miembros);
        this.cargando.set(false);
        if (this.esCapitan() && !this.vistaHistorica()) {
          this.cargarSolicitudes();
        }
      },
      error: (err) => {
        this.error.set(err?.status === 404
          ? 'El equipo consultado no existe.'
          : 'No se pudo cargar la plantilla del equipo.');
        this.cargando.set(false);
      }
    });
  }

  /** Reintento del criterio "error de carga" de RF-08. */
  reintentar(): void {
    if (!this.equipo()) {
      this.cargarEquipo();
    }
    this.cargarMiembros();
  }

  cambiarRol(miembro: MiembroEquipo, nuevoRol: string): void {
    if (nuevoRol === miembro.rol) {
      return;
    }
    this.guardandoId.set(miembro.usuarioId);
    this.teamsService.cambiarRol(this.equipoId, miembro.usuarioId, { nuevoRol }).subscribe({
      next: () => {
        this.guardandoId.set(null);
        this.cargarMiembros();
      },
      error: (err) => {
        this.guardandoId.set(null);
        const mensaje = err?.error?.message ?? 'No se pudo cambiar el rol.';
        alert(mensaje);
        this.cargarMiembros();
      }
    });
  }

  // ---------- invitaciones múltiples ----------

  estaSeleccionado(jugadorId: number): boolean {
    return this.seleccionados().some((s) => s.id === jugadorId);
  }

  toggleSeleccion(jugador: JugadorDisponible): void {
    if (this.estaSeleccionado(jugador.id)) {
      this.quitarSeleccion(jugador.id);
      return;
    }
    this.seleccionados.update((lista) => [
      ...lista,
      { id: jugador.id, nombre: jugador.nombre, rol: 'TITULAR' }
    ]);
    this.resumenInvitaciones.set(null);
  }

  quitarSeleccion(jugadorId: number): void {
    this.seleccionados.update((lista) => lista.filter((s) => s.id !== jugadorId));
  }

  cambiarRolSeleccion(jugadorId: number, rol: string): void {
    this.seleccionados.update((lista) =>
      lista.map((s) => (s.id === jugadorId ? { ...s, rol } : s)));
  }

  /** Envía una invitación por cada seleccionado, cada uno con su rol. */
  invitarSeleccionados(): void {
    const lista = this.seleccionados();
    if (lista.length === 0 || this.invitando()) {
      return;
    }
    this.invitando.set(true);
    this.errorInvitar.set(null);
    this.resumenInvitaciones.set(null);
    const mensaje = this.mensajeInvitacion().trim() || null;

    forkJoin(lista.map((s) =>
      this.teamsService.invitar(this.equipoId, {
        jugadorId: s.id,
        rolPropuesto: s.rol,
        mensaje
      }).pipe(
        map(() => ({ seleccion: s, error: null as string | null })),
        catchError((err) => of({
          seleccion: s,
          error: (err?.error?.message ?? 'no se pudo invitar') as string | null
        }))
      )
    )).subscribe((resultados) => {
      this.invitando.set(false);
      const fallidos = resultados.filter((r) => r.error !== null);
      const enviados = resultados.length - fallidos.length;
      // Los fallidos quedan seleccionados para reintentar o quitar.
      this.seleccionados.set(fallidos.map((f) => f.seleccion));
      if (enviados > 0) {
        this.mensajeInvitacion.set('');
        this.resumenInvitaciones.set(
          enviados === 1 ? 'Invitación enviada.' : `${enviados} invitaciones enviadas.`);
      }
      if (fallidos.length > 0) {
        this.errorInvitar.set(fallidos
          .map((f) => `${f.seleccion.nombre}: ${f.error}`)
          .join(' · '));
      }
    });
  }

  // ---------- solicitudes de unión ----------

  private cargarSolicitudes(): void {
    this.teamsService.solicitudesPendientes(this.equipoId).subscribe({
      next: (solicitudes) => this.solicitudes.set(solicitudes),
      error: () => this.solicitudes.set([])
    });
  }

  responderSolicitud(solicitud: SolicitudUnion, aceptar: boolean): void {
    this.respondiendoSolicitud.set(solicitud.id);
    this.errorSolicitudes.set(null);
    this.teamsService.responderSolicitud(solicitud.id, aceptar).subscribe({
      next: () => {
        this.respondiendoSolicitud.set(null);
        this.solicitudes.update((lista) => lista.filter((s) => s.id !== solicitud.id));
        if (aceptar) {
          this.cargarMiembros();
        }
      },
      error: (err) => {
        this.respondiendoSolicitud.set(null);
        this.errorSolicitudes.set(err?.error?.message ?? 'No se pudo responder la solicitud.');
      }
    });
  }

  /** RF-10: abre la confirmación de expulsión para un integrante activo. */
  iniciarExpulsion(miembro: MiembroEquipo): void {
    this.miembroAExpulsar.set(miembro);
    this.causaExpulsion.set('');
    this.errorExpulsion.set(null);
  }

  /** RF-10: cancelar la confirmación no realiza la baja. */
  cancelarExpulsion(): void {
    this.miembroAExpulsar.set(null);
    this.causaExpulsion.set('');
    this.errorExpulsion.set(null);
  }

  confirmarExpulsion(): void {
    const miembro = this.miembroAExpulsar();
    if (!miembro || this.expulsando()) {
      return;
    }
    const causa = this.causaExpulsion().trim();
    if (!causa) {
      this.errorExpulsion.set('La causa de la expulsión es obligatoria.');
      return;
    }
    this.expulsando.set(true);
    this.errorExpulsion.set(null);
    this.teamsService.expulsar(this.equipoId, miembro.usuarioId, { causa }).subscribe({
      next: () => {
        this.expulsando.set(false);
        this.cancelarExpulsion();
        this.cargarMiembros();
      },
      error: (err) => {
        this.expulsando.set(false);
        this.errorExpulsion.set(err?.error?.message ?? 'No se pudo expulsar al integrante.');
      }
    });
  }

  disolverEquipo(): void {
    if (!this.confirmaDisolucion() || this.disolviendo() || this.equipoDisuelto()) {
      return;
    }
    this.disolviendo.set(true);
    this.errorDisolucion.set(null);
    this.teamsService.disolver(this.equipoId, {
      confirmacion: true,
      motivo: this.motivoDisolucion().trim() || null
    }).subscribe({
      next: (equipo) => {
        this.disolviendo.set(false);
        this.equipoDisuelto.set(equipo);
      },
      error: (err) => {
        this.disolviendo.set(false);
        this.errorDisolucion.set(err?.error?.message ?? 'No se pudo disolver el equipo.');
      }
    });
  }

  /** Cada tecla dispara la búsqueda con debounce (sin botón obligatorio). */
  onTextoBusqueda(valor: string): void {
    this.textoBusqueda.set(valor);
    this.busquedaEnVivo$.next();
  }

  buscarJugadores(): void {
    this.buscando.set(true);
    this.errorBusqueda.set(null);
    this.teamsService.buscarJugadores(
      this.equipoId, this.textoBusqueda().trim(), this.juegoIdBusqueda(), this.soloDisponibles()
    ).subscribe({
      next: (pagina) => {
        // El backend ahora pagina; el template itera la lista de la página.
        this.resultados.set(pagina.items);
        this.buscando.set(false);
      },
      error: (err) => {
        this.buscando.set(false);
        this.errorBusqueda.set(err?.error?.message ?? 'No se pudo completar la busqueda.');
      }
    });
  }

}
