import { Component, OnInit, computed, effect, inject, input, output, signal } from '@angular/core';

import { AjustePartida, CrearTorneoRequest, Torneo } from '../../../../models/tournament.model';
import { League, Season } from '../../../../models/league.model';
import { TournamentsService } from '../../services/tournaments.service';
import { LeaguesService } from '../../../leagues/services/leagues.service';
import { CompetitiveProfileService } from '../../../../core/services/competitive-profile.service';
import { AuthService } from '../../../../core/services/auth.service';

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
  imports: [],
  templateUrl: './tournament-wizard.component.html',
  styleUrl: './tournament-wizard.component.scss'
})
export class TournamentWizardComponent implements OnInit {
  private readonly tournamentsService = inject(TournamentsService);
  private readonly leaguesService = inject(LeaguesService);
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
  readonly ajustesSugeridos = [
    'Modo de juego', 'Arena / Mapa', 'Duración', 'Puntaje máximo', 'Región', 'Overtime'
  ];

  agregarAjuste(clave = ''): void {
    if (clave && this.ajustes().some((a) => a.clave === clave)) {
      return;
    }
    this.ajustes.update((lista) => [...lista, { clave, valor: '' }]);
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
  private static readonly DESCRIPCIONES: [RegExp, string][] = [
    [/doble/i, 'Los perdedores siguen en la llave inferior; se queda fuera quien pierde dos veces.'],
    [/grupo/i, 'Fase de grupos y los mejores avanzan a una llave eliminatoria.'],
    [/robin/i, 'Todos contra todos: cada equipo enfrenta al resto de su grupo.'],
    [/suizo/i, 'Sin eliminación: cada ronda empareja rivales con marcas similares.'],
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

  /** El paso actual está completo y se puede avanzar. */
  readonly pasoValido = computed(() => {
    switch (this.paso()) {
      case 1:
        return this.nombre().trim().length > 0
          && (this.ligaId() === null || this.temporadaId() !== null);
      case 2:
        return !!this.formato() && this.tamano() > 0 && this.cupo() >= 2;
      case 3:
        return !!this.fechaInicio() && new Date(this.fechaInicio()) > new Date();
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

    this.perfilService.listarFormatos().subscribe({
      next: (catalogo) => {
        if (catalogo.length > 0) {
          this.formatos.set(catalogo.map((f) => f.nombre));
          if (!catalogo.some((f) => f.nombre === this.formato())) {
            this.formato.set(catalogo[0].nombre);
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
}
