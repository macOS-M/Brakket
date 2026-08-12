import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  computed,
  effect,
  inject,
  input,
  output,
  signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { FechaInputComponent } from '../../../../shared/components/fecha-input/fecha-input.component';
import { AjustePartida, CrearTorneoRequest, Torneo } from '../../../../models/tournament.model';
import { Juego } from '../../../../models/juego.model';
import { League, Season } from '../../../../models/league.model';
import { TournamentsService } from '../../services/tournaments.service';
import { GamesService } from '../../../games/services/games.service';
import { LeaguesService } from '../../../leagues/services/leagues.service';
import { CompetitiveProfileService } from '../../../../core/services/competitive-profile.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ahoraCostaRica } from '../../../../shared/utils/hora-costa-rica';

/**
 * Wizard de creación de torneo (RF-24), 3 pasos como la referencia
 * Challenger Mode: General → Equipos → Fecha. El torneo puede ser
 * comunitario (solo el juego) u hospedarse en una temporada de una liga
 * del propio usuario. El perfil competitivo del juego, si existe, acota
 * los tamaños de equipo; sin perfil valen los estándares 1v1…5v5.
 */
@Component({
  selector: 'app-tournament-wizard',
  standalone: true,
  imports: [FormsModule, FechaInputComponent],
  templateUrl: './tournament-wizard.component.html',
  styleUrl: './tournament-wizard.component.scss'
})
export class TournamentWizardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('modalRef') private modalRef!: ElementRef<HTMLElement>;
  /** A quién devolverle el foco al cerrar (el botón "Crear" del hub). */
  private focoPrevio: HTMLElement | null = null;

  private readonly tournamentsService = inject(TournamentsService);
  private readonly leaguesService = inject(LeaguesService);
  private readonly gamesService = inject(GamesService);
  private readonly perfilService = inject(CompetitiveProfileService);
  private readonly auth = inject(AuthService);

  readonly juegoId = input.required<number>();
  readonly juegoNombre = input.required<string>();
  readonly juegoImagen = input<string | null>(null);
  /** Liga propia preseleccionada (al crear desde el detalle de la liga). */
  readonly ligaIdInicial = input<number | null>(null);

  readonly cerrado = output<void>();
  readonly creado = output<Torneo>();

  readonly paso = signal<1 | 2 | 3>(1);
  readonly pasos = ['General', 'Equipos', 'Fecha'];

  // Paso 1 — General
  readonly nombre = signal('');
  readonly descripcion = signal('');
  readonly publico = signal(true);
  readonly ligaId = signal<number | null>(null);
  readonly temporadaId = signal<number | null>(null);

  // Paso 2 — Equipos
  readonly formato = signal('Eliminación directa');
  readonly tamano = signal(5);
  readonly cupo = signal(8);

  // Paso 3 — Fecha y configuración avanzada
  readonly fechaInicio = signal('');
  readonly premio = signal('');

  /**
   * Ajustes de partida (referencia "Game settings" de CM): el contrato que
   * ambos capitanes deben aplicar al crear la lobby privada en el juego.
   */
  readonly ajustes = signal<AjustePartida[]>([]);

  /** Ficha del juego (RAWG): alimenta las sugerencias de ajustes. */
  private readonly juegoFicha = signal<Juego | null>(null);

  readonly ajustesSugeridos = computed(() => {
    const base = ['Modo de juego', 'Arena / Mapa', 'Duración', 'Puntaje máximo', 'Región', 'Overtime'];
    if (this.juegoFicha()?.plataformas) {
      base.push('Plataforma');
    }
    return base;
  });

  agregarAjuste(clave = ''): void {
    if (clave && this.ajustes().some((a) => a.clave === clave)) {
      return;
    }
    this.ajustes.update((lista) => [...lista, { clave, valor: this.valorSugerido(clave) }]);
  }

  /** Prefill con datos reales del juego cuando la ficha RAWG los trae. */
  private valorSugerido(clave: string): string {
    const juego = this.juegoFicha();
    if (!juego) {
      return '';
    }
    if (clave === 'Plataforma') {
      return juego.plataformas ?? '';
    }
    if (clave === 'Modo de juego' && juego.etiquetas) {
      const modos = juego.etiquetas.split(', ')
        .filter((t) => ['Multiplayer', 'PvP', 'Co-op', 'Online Co-Op', 'Singleplayer'].includes(t));
      return modos.join(', ');
    }
    return '';
  }

  quitarAjuste(indice: number): void {
    this.ajustes.update((lista) => lista.filter((_, i) => i !== indice));
  }

  editarAjuste(indice: number, campo: 'clave' | 'valor', valor: string): void {
    this.ajustes.update((lista) =>
      lista.map((a, i) => (i === indice ? { ...a, [campo]: valor } : a)));
  }

  private readonly ligasDisponibles = signal<League[]>([]);

  /**
   * Ligas propias del juego. Computado sobre auth.usuario() para que se
   * recalcule si /me responde después de abrir el wizard: leer el id una
   * sola vez en ngOnInit dejaba la lista vacía en ese caso.
   */
  readonly misLigas = computed(() => {
    const usuarioId = Number(this.auth.usuario()?.id);
    return this.ligasDisponibles().filter(
      (l) => l.comisionadoId === usuarioId && l.juegoId === this.juegoId());
  });

  readonly temporadas = signal<Season[]>([]);
  readonly formatos = signal<string[]>([
    'Eliminación directa',
    'Doble eliminación',
    'Round robin',
    'Fase de grupos y eliminación',
    'Suizo'
  ]);
  readonly tamanosPermitidos = signal<number[]>([1, 2, 3, 4, 5]);
  readonly cupos = [2, 4, 8, 16, 32, 64];

  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);

  /**
   * Descripciones de los formatos (referencia "Select bracket template" de
   * Challenger Mode). Se resuelven por coincidencia laxa sobre el nombre
   * que venga del catálogo.
   */
  // Mismo orden de precedencia que FormatoTorneo.interpretar (backend):
  // grupo/robin/suizo antes que doble, por los nombres compuestos.
  private static readonly DESCRIPCIONES: [RegExp, string][] = [
    [/grupo/i, 'Fase de grupos y los mejores avanzan a una llave eliminatoria.'],
    [/robin/i, 'Todos contra todos: cada equipo enfrenta al resto de su grupo.'],
    [/suizo/i, 'Sin eliminación: cada ronda empareja rivales con marcas similares.'],
    [/doble/i, 'Los perdedores siguen en la llave inferior; se queda fuera quien pierde dos veces.'],
    [/elim/i, 'El formato clásico: quien pierde queda eliminado, hasta coronar al campeón.']
  ];

  descripcionDe(formato: string): string {
    const par = TournamentWizardComponent.DESCRIPCIONES.find(([regex]) => regex.test(formato));
    return par ? par[1] : 'Formato competitivo del catálogo de la plataforma.';
  }

  /** DOBLE_ELIMINACION → Doble eliminación (por si el catálogo viene crudo). */
  etiquetaDe(formato: string): string {
    const texto = formato.replaceAll('_', ' ').toLowerCase();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }

  /**
   * El motor genera los cinco formatos del catálogo (DD-05 cerrada). El
   * gate queda por si el catálogo algún día trae un formato sin motor:
   * ese seguiría marcado "Pronto".
   */
  soportado(formato: string): boolean {
    const plano = formato.normalize('NFD').replace(/\p{M}/gu, '').toUpperCase();
    return /DOBLE|GRUPO|ROBIN|SUIZO|SWISS|ELIMINACION|DIRECTA/.test(plano);
  }

  /** La fase de grupos necesita cupo para armar grupos y llave (≥ 4). */
  esFormatoGrupos(): boolean {
    return /GRUPO/.test(this.formato().normalize('NFD').replace(/\p{M}/gu, '').toUpperCase());
  }

  /** El paso actual está completo y se puede avanzar. */
  readonly pasoValido = computed(() => {
    switch (this.paso()) {
      case 1:
        return this.nombre().trim().length > 0
          && (this.ligaId() === null || this.temporadaId() !== null);
      case 2:
        return !!this.formato() && this.tamano() > 0 && this.cupo() >= 2
          && (!this.esFormatoGrupos() || this.cupo() >= 4);
      case 3:
        // "Futura" respecto a Costa Rica: si no, desde otro huso el wizard
        // rechazaba horas válidas o aceptaba horas ya pasadas allá.
        return !!this.fechaInicio() && new Date(this.fechaInicio()) > ahoraCostaRica();
    }
  });

  private preseleccionAplicada = false;

  constructor() {
    // La preselección espera a que /me y el listado de ligas respondan;
    // se aplica una sola vez para no pisar un cambio manual a Comunitario.
    effect(() => {
      const inicial = this.ligaIdInicial();
      if (!this.preseleccionAplicada && inicial
        && this.misLigas().some((l) => l.id === inicial)) {
        this.preseleccionAplicada = true;
        this.elegirLiga(String(inicial));
      }
    });
  }

  ngOnInit(): void {
    this.leaguesService.list().subscribe({
      next: (ligas) => this.ligasDisponibles.set(ligas),
      error: () => this.ligasDisponibles.set([])
    });

    this.gamesService.obtenerPorId(this.juegoId()).subscribe({
      next: (juego) => this.juegoFicha.set(juego),
      error: () => this.juegoFicha.set(null)
    });

    this.perfilService.listarFormatos().subscribe({
      next: (catalogo) => {
        if (catalogo.length > 0) {
          this.formatos.set(catalogo.map((f) => f.nombre));
          // El seleccionado debe existir en el catálogo y estar soportado.
          const actualValido = catalogo.some(
            (f) => f.nombre === this.formato() && this.soportado(f.nombre));
          if (!actualValido) {
            const primero = catalogo.find((f) => this.soportado(f.nombre)) ?? catalogo[0];
            this.formato.set(primero.nombre);
          }
        }
      },
      error: () => undefined
    });

    // Perfil competitivo como curaduría opcional: si existe, acota tamaños.
    this.perfilService.obtenerPorJuego(this.juegoId()).subscribe({
      next: (perfil) => {
        if (perfil?.activo) {
          const permitidos: number[] = [];
          for (let n = perfil.plantillaMinima; n <= Math.min(perfil.plantillaMaxima, 10); n++) {
            permitidos.push(n);
          }
          if (permitidos.length > 0) {
            this.tamanosPermitidos.set(permitidos);
            this.tamano.set(permitidos[permitidos.length - 1]);
          }
        }
      },
      error: () => undefined
    });
  }

  elegirLiga(valor: string): void {
    const ligaId = valor ? Number(valor) : null;
    this.ligaId.set(ligaId);
    this.temporadaId.set(null);
    this.temporadas.set([]);
    if (ligaId) {
      this.leaguesService.listSeasons(ligaId).subscribe({
        next: (temporadas) => this.temporadas.set(temporadas),
        error: () => this.temporadas.set([])
      });
    }
  }

  siguiente(): void {
    if (!this.pasoValido()) {
      return;
    }
    this.paso.update((p) => (p < 3 ? ((p + 1) as 2 | 3) : p));
  }

  volver(): void {
    this.paso.update((p) => (p > 1 ? ((p - 1) as 1 | 2) : p));
  }

  crear(): void {
    if (!this.pasoValido() || this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set(null);

    const request: CrearTorneoRequest = {
      nombre: this.nombre().trim(),
      juegoId: this.juegoId(),
      temporadaId: this.temporadaId(),
      formato: this.formato(),
      tamanoEquipo: this.tamano(),
      maxEquipos: this.cupo(),
      fechaInicio: this.fechaInicio(),
      publico: this.publico(),
      descripcion: this.descripcion().trim() || null,
      premio: this.premio().trim() || null,
      ajustesPartida: this.ajustes()
        .map((a) => ({ clave: a.clave.trim(), valor: a.valor.trim() }))
        .filter((a) => a.clave && a.valor)
    };

    this.tournamentsService.crear(request).subscribe({
      next: (torneo) => this.creado.emit(torneo),
      error: (err) => {
        this.guardando.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo crear el torneo.');
      }
    });
  }

  cerrar(): void {
    if (!this.guardando()) {
      this.cerrado.emit();
    }
  }

  // ---------- gestión de foco del modal (WCAG 2.4.3 / patrón dialog) ----------

  /** Al abrir, el foco entra al modal; sin esto, Tab y Escape siguen
   *  operando la página tapada por el telón. */
  ngAfterViewInit(): void {
    this.focoPrevio = document.activeElement as HTMLElement | null;
    this.modalRef.nativeElement.focus();
  }

  ngOnDestroy(): void {
    this.focoPrevio?.focus();
  }

  /** Trampa de Tab: el foco circula dentro del modal. */
  atraparTab(event: Event): void {
    const e = event as KeyboardEvent;
    const focusables = this.modalRef.nativeElement.querySelectorAll<HTMLElement>(
      'button:not([disabled]), input, select, textarea, summary, [href]');
    if (focusables.length === 0) {
      return;
    }
    const primero = focusables[0];
    const ultimo = focusables[focusables.length - 1];
    if (e.shiftKey && (document.activeElement === primero
        || document.activeElement === this.modalRef.nativeElement)) {
      e.preventDefault();
      ultimo.focus();
    } else if (!e.shiftKey && document.activeElement === ultimo) {
      e.preventDefault();
      primero.focus();
    }
  }

  /** Radios con flechas (patrón radiogroup): mover selección y foco. */
  moverTamano(delta: number, event: Event): void {
    event.preventDefault();
    const opciones = this.tamanosPermitidos();
    const i = opciones.indexOf(this.tamano());
    const destino = opciones[(i + delta + opciones.length) % opciones.length];
    this.tamano.set(destino);
    this.enfocarRadioActivo('.opciones-tamano');
  }

  moverFormato(delta: number, event: Event): void {
    event.preventDefault();
    const soportados = this.formatos().filter((f) => this.soportado(f));
    if (soportados.length === 0) {
      return;
    }
    const i = soportados.indexOf(this.formato());
    const destino = soportados[(i + delta + soportados.length) % soportados.length];
    this.formato.set(destino);
    this.enfocarRadioActivo('.formatos');
  }

  private enfocarRadioActivo(selectorGrupo: string): void {
    // El cambio de selección re-renderiza; el foco sigue a la opción activa.
    setTimeout(() => {
      this.modalRef.nativeElement
        .querySelector<HTMLElement>(`${selectorGrupo} [aria-checked="true"]`)
        ?.focus();
    });
  }
}
