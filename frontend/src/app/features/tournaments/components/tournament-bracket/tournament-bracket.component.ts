import {
  Component,
  DestroyRef,
  ElementRef,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  viewChild
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { Partida } from '../../../../models/tournament.model';
import {
  ApelacionResponse,
  ApelarRequest,
  DecisionDisputa,
  DisputaResponse,
  ImpugnarResultadoRequest,
  ResolverApelacionRequest,
  ResolverDisputaRequest
} from '../../../../models/disputa.model';
import { RegistrarCasoEspecialRequest, TipoCasoEspecial } from '../../../../models/caso-especial.model';
import { EvidenciaResponse } from '../../../../models/evidencia.model';
import { TournamentsService } from '../../services/tournaments.service';
import { DisputesService } from '../../../disputes/services/disputes.service';
import { UploadsService } from '../../../../shared/services/uploads.service';

interface Ronda {
  numero: number;
  etiqueta: string;
  partidas: Partida[];
}

interface RondaPlaceholder {
  numero: number;
  etiqueta: string;
  cruces: number[];
}

/** Fila de la tabla de posiciones (round robin, suizo y grupos). */
interface Posicion {
  equipoId: number;
  nombre: string;
  logo: string | null;
  jugadas: number;
  ganadas: number;
  perdidas: number;
  diferencia: number;
}

/** Un grupo de la fase de grupos: su tabla y sus jornadas. */
interface Grupo {
  indice: number;
  letra: string;
  tabla: Posicion[];
  jornadas: Ronda[];
}

/** Un riel de la llave: el camino SVG entre una partida y su siguiente cruce. */
interface Riel {
  id: string;
  d: string;
  campeon: boolean;
  mio: boolean;
  pulso: boolean;
}

export interface MarcadorEvent {
  partida: Partida;
  marcadorA: number;
  marcadorB: number;
  /** true cuando lo envía el organizador como resolución, no como reporte. */
  resolucion: boolean;
}

/** Impugnación de un resultado ya finalizado (RF-30). */
export interface ImpugnarEvent {
  partida: Partida;
  request: ImpugnarResultadoRequest;
}

/** Descanso, avance automático o abandono sobre una partida (RF-28). */
export interface CasoEspecialEvent {
  partida: Partida;
  request: RegistrarCasoEspecialRequest;
}

/** Forma de dibujar el formato: árbol único, doble llave, liga o grupos. */
type TipoVista = 'ARBOL' | 'DOBLE' | 'LIGA' | 'GRUPOS';

/**
 * La llave del torneo (RF-26/27), estilo Challenger Mode. La vista depende
 * del formato: árbol de eliminación (columnas por ronda y rieles SVG),
 * doble eliminación (llave superior, inferior y gran final), round robin
 * y suizo (tabla de posiciones + jornadas) o fase de grupos (un bloque
 * por grupo y la llave final).
 *
 * Los rieles se dibujan midiendo el DOM real (las tarjetas cambian de
 * alto al abrir formularios) y siguen los enlaces reales de avance
 * (siguientePartidaId), así valen también para la doble eliminación.
 */
@Component({
  selector: 'app-tournament-bracket',
  standalone: true,
  imports: [NgTemplateOutlet],
  templateUrl: './tournament-bracket.component.html',
  styleUrl: './tournament-bracket.component.scss'
})
export class TournamentBracketComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly tournamentsService = inject(TournamentsService);
  private readonly disputesService = inject(DisputesService);
  private readonly uploadsService = inject(UploadsService);

  readonly partidas = input.required<Partida[]>();
  /** Cupo del torneo: dibuja la llave tentativa antes de generarse. */
  readonly maxEquipos = input.required<number>();
  /** Código o nombre del formato; define la vista. */
  readonly formato = input<string>('ELIMINACION_DIRECTA');
  /** Campeón según el torneo (para coronar tabla o final). */
  readonly campeonEquipoId = input<number | null>(null);
  readonly misEquipos = input<number[]>([]);
  readonly esGestor = input(false);
  /** Organizador o árbitro de este torneo: habilita el botón de RF-28. */
  readonly puedeCasoEspecial = input(false);
  readonly enCurso = input(false);
  readonly ocupado = input(false);
  readonly enviarMarcador = output<MarcadorEvent>();
  readonly confirmar = output<Partida>();
  readonly rechazar = output<Partida>();
  readonly impugnar = output<ImpugnarEvent>();
  /** Cuando resolver una disputa o apelación cambió el resultado de la partida. */
  readonly disputaResuelta = output<void>();
  readonly casoEspecialRegistrado = output<void>();

  /** Partida con el formulario de marcador abierto. */
  readonly reportando = signal<number | null>(null);
  readonly marcadorA = signal(0);
  readonly marcadorB = signal(0);
  readonly errorLocal = signal<string | null>(null);

  // ---------- RF-30: impugnar un resultado finalizado ----------

  /** Partida con el formulario de impugnación abierto. */
  readonly impugnando = signal<number | null>(null);
  readonly motivoImpugnar = signal('');
  readonly descripcionImpugnar = signal('');
  readonly evidenciaImpugnar = signal('');
  readonly errorImpugnar = signal<string | null>(null);

  // ---------- RF-31/32: disputa de una partida (evidencia, resolver, apelar) ----------

  /** Partida cuyo panel de disputa está desplegado (null = ninguna). */
  readonly verEvidencia = signal<number | null>(null);
  readonly cargandoEvidencia = signal(false);
  /** La disputa más reciente de esa partida, sea cual sea su estado. */
  readonly disputaActual = signal<DisputaResponse | null>(null);
  readonly evidencias = signal<EvidenciaResponse[]>([]);
  readonly errorEvidencia = signal<string | null>(null);
  readonly nuevaEvidenciaUrl = signal('');
  readonly nuevaEvidenciaDescripcion = signal('');
  readonly subiendoArchivo = signal(false);
  readonly enviandoEvidencia = signal(false);

  // RF-32: resolver la disputa.
  readonly resolviendoDisputa = signal(false);
  readonly decisionResolver = signal<DecisionDisputa>('MANTENER');
  readonly justificacionResolver = signal('');
  readonly sancionResolver = signal('');
  readonly equipoGanadorResolver = signal<number | null>(null);
  readonly enviandoResolucion = signal(false);
  readonly errorResolucion = signal<string | null>(null);

  // RF-32: apelar la resolución.
  readonly apelacionActual = signal<ApelacionResponse | null>(null);
  readonly apelando = signal(false);
  readonly motivoApelar = signal('');
  readonly enviandoApelacion = signal(false);
  readonly errorApelacion = signal<string | null>(null);

  // RF-32: el comisionado resuelve la apelación.
  readonly resolviendoApelacion = signal(false);
  readonly decisionFinalApelacion = signal('');
  readonly equipoGanadorApelacion = signal<number | null>(null);
  readonly enviandoResolucionApelacion = signal(false);
  readonly errorResolucionApelacion = signal<string | null>(null);

  // ---------- RF-28: descansos, avances y abandonos ----------

  /** Partida con el formulario de caso especial abierto. */
  readonly registrandoCaso = signal<number | null>(null);
  readonly tipoCaso = signal<TipoCasoEspecial>('DESCANSO');
  readonly justificacionCaso = signal('');
  readonly evidenciaCaso = signal('');
  readonly equipoGanadorCaso = signal<number | null>(null);
  readonly errorCaso = signal<string | null>(null);
  readonly enviandoCaso = signal(false);

  /** Rieles SVG calculados midiendo las tarjetas ya pintadas. */
  readonly rieles = signal<Riel[]>([]);
  readonly lienzoAncho = signal(0);
  readonly lienzoAlto = signal(0);

  /** Partidas que acaban de cambiar de estado (flash + pulso del riel). */
  readonly recienActualizadas = signal<ReadonlySet<number>>(new Set());
  /** true durante la cascada de entrada tras generarse la llave en vivo. */
  readonly generacionReciente = signal(false);
  /** Credencial de lobby copiada al portapapeles ('id-lobby' | 'id-clave'). */
  readonly copiado = signal<string | null>(null);

  private readonly lienzo = viewChild<ElementRef<HTMLElement>>('lienzo');
  private estadosPrevios: Map<number, string> | null = null;
  private observador: ResizeObserver | null = null;
  private ultimaGeometria = '';

  /**
   * La vista que corresponde al formato (laxa y en el MISMO orden de
   * precedencia que FormatoTorneo.interpretar del backend: GRUPO y ROBIN
   * antes que DOBLE, para que "round robin doble" caiga en liga).
   */
  readonly tipo = computed<TipoVista>(() => {
    const plano = this.formato().normalize('NFD').replace(/\p{M}/gu, '').toUpperCase();
    if (/GRUPO/.test(plano)) {
      return 'GRUPOS';
    }
    if (/ROBIN|SUIZO|SWISS/.test(plano)) {
      return 'LIGA';
    }
    if (/DOBLE/.test(plano)) {
      return 'DOBLE';
    }
    return 'ARBOL';
  });

  /** Las jornadas de una liga se llaman así solo en round robin. */
  readonly nombreJornada = computed(() =>
    /ROBIN/i.test(this.formato()) ? 'Jornada' : 'Ronda');

  // ---------- agrupaciones por vista ----------

  private rondasDe(lista: Partida[], etiqueta: (numero: number, total: number) => string): Ronda[] {
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
        etiqueta: etiqueta(numero, total),
        partidas: partidas.sort((a, b) => a.orden - b.orden)
      }));
  }

  /** Árbol único (eliminación directa). */
  readonly rondas = computed<Ronda[]>(() =>
    this.rondasDe(this.partidas(), (n, t) => this.etiquetaRonda(n, t)));

  readonly rondasSuperior = computed<Ronda[]>(() =>
    this.rondasDe(
      this.partidas().filter((p) => p.fase === 'WINNERS'),
      (n, t) => (n === t ? 'Final superior' : `Ronda ${n}`)));

  readonly rondasInferior = computed<Ronda[]>(() =>
    this.rondasDe(
      this.partidas().filter((p) => p.fase === 'LOSERS'),
      (n, t) => (n === t ? 'Final inferior' : `Inferior ${n}`)));

  readonly granFinal = computed<Partida | null>(() =>
    this.partidas().find((p) => p.fase === 'GRAN_FINAL') ?? null);

  /** Round robin y suizo: jornadas planas. */
  readonly jornadas = computed<Ronda[]>(() =>
    this.rondasDe(this.partidas(), (n) => `${this.nombreJornada()} ${n}`));

  /** Tabla general de la liga (round robin / suizo). */
  readonly tablaLiga = computed<Posicion[]>(() => this.tabla(this.partidas()));

  readonly grupos = computed<Grupo[]>(() => {
    const deGrupos = this.partidas().filter((p) => p.fase === 'GRUPOS');
    const indices = [...new Set(deGrupos.map((p) => p.grupo ?? 0))].sort((a, b) => a - b);
    return indices.map((indice) => {
      const propias = deGrupos.filter((p) => (p.grupo ?? 0) === indice);
      return {
        indice,
        letra: String.fromCharCode(65 + indice),
        tabla: this.tabla(propias),
        jornadas: this.rondasDe(propias, (n) => `Jornada ${n}`)
      };
    });
  });

  /** Llave posterior a los grupos. */
  readonly llaveFinal = computed<Ronda[]>(() =>
    this.rondasDe(
      this.partidas().filter((p) => p.fase === 'ELIMINACION'),
      (n, t) => this.etiquetaRonda(n, t)));

  /**
   * Tabla de posiciones desde las partidas cerradas: victorias, diferencia
   * y puntos a favor (mismos criterios que el backend). Un bye vale una
   * victoria sin marcador.
   */
  private tabla(lista: Partida[]): Posicion[] {
    const filas = new Map<number, Posicion>();
    const asegurar = (id: number | null, nombre: string | null, logo: string | null) => {
      if (id !== null && !filas.has(id)) {
        filas.set(id, {
          equipoId: id, nombre: nombre ?? '—', logo,
          jugadas: 0, ganadas: 0, perdidas: 0, diferencia: 0
        });
      }
    };
    const favor = new Map<number, number>();
    for (const p of lista) {
      asegurar(p.equipoAId, p.equipoANombre, p.equipoALogo);
      asegurar(p.equipoBId, p.equipoBNombre, p.equipoBLogo);
      if (p.estado !== 'FINALIZADA' || p.ganadorEquipoId === null) {
        continue;
      }
      for (const lado of ['A', 'B'] as const) {
        const id = lado === 'A' ? p.equipoAId : p.equipoBId;
        if (id === null) {
          continue;
        }
        const fila = filas.get(id)!;
        fila.jugadas++;
        if (id === p.ganadorEquipoId) {
          fila.ganadas++;
        } else {
          fila.perdidas++;
        }
        if (p.marcadorA !== null && p.marcadorB !== null) {
          const propio = lado === 'A' ? p.marcadorA : p.marcadorB;
          const rival = lado === 'A' ? p.marcadorB : p.marcadorA;
          fila.diferencia += propio - rival;
          favor.set(id, (favor.get(id) ?? 0) + propio);
        }
      }
    }
    return [...filas.values()].sort((a, b) =>
      b.ganadas - a.ganadas
      || b.diferencia - a.diferencia
      || (favor.get(b.equipoId) ?? 0) - (favor.get(a.equipoId) ?? 0)
      || a.equipoId - b.equipoId);
  }

  /** Llave tentativa según el cupo, cuando el bracket aún no existe. */
  readonly rondasPlaceholder = computed<RondaPlaceholder[]>(() => {
    let size = 2;
    while (size < Math.min(this.maxEquipos(), 64)) {
      size <<= 1;
    }
    const total = Math.log2(size);
    const rondas: RondaPlaceholder[] = [];
    for (let r = 1; r <= total; r++) {
      rondas.push({
        numero: r,
        etiqueta: this.etiquetaRonda(r, total),
        cruces: Array.from({ length: size >> r }, (_, i) => i)
      });
    }
    return rondas;
  });

  /** Equipo campeón: lo dice el torneo o, si no, la final del árbol. */
  readonly campeonId = computed<number | null>(() => {
    if (this.campeonEquipoId() !== null) {
      return this.campeonEquipoId();
    }
    const culmen = this.partidaCulmen();
    return culmen?.estado === 'FINALIZADA' ? culmen.ganadorEquipoId : null;
  });

  /** La partida que corona: gran final, final de la llave o final del árbol. */
  private partidaCulmen(): Partida | null {
    const lista = this.partidas();
    if (lista.length === 0) {
      return null;
    }
    if (this.tipo() === 'DOBLE') {
      return this.granFinal();
    }
    if (this.tipo() === 'LIGA') {
      return null;
    }
    const arbol = this.tipo() === 'GRUPOS'
      ? lista.filter((p) => p.fase === 'ELIMINACION')
      : lista;
    if (arbol.length === 0) {
      return null;
    }
    const total = Math.max(...arbol.map((p) => p.ronda));
    return arbol.find((p) => p.ronda === total) ?? null;
  }

  /** Índice global de tarjeta (orden de dibujo) para la cascada. */
  readonly indicesCascada = computed<Map<number, number>>(() => {
    const indices = new Map<number, number>();
    let i = 0;
    for (const p of this.partidas()) {
      indices.set(p.id, i++);
    }
    return indices;
  });

  constructor() {
    effect(() => {
      const lista = this.partidas();
      const previos = this.estadosPrevios;
      const actuales = new Map(lista.map((p) => [p.id, p.estado] as const));

      if (previos !== null) {
        if (previos.size === 0 && lista.length > 0) {
          this.iniciarCascada();
        } else if (previos.size > 0) {
          const cambiadas = lista
            .filter((p) => previos.has(p.id) && previos.get(p.id) !== p.estado)
            .map((p) => p.id);
          if (cambiadas.length > 0) {
            this.marcarActualizadas(cambiadas);
          }
        }
      }
      this.estadosPrevios = actuales;
    });

    effect(() => {
      this.partidas();
      this.maxEquipos();
      this.reportando();
      this.impugnando();
      this.verEvidencia();
      this.resolviendoDisputa();
      this.apelando();
      this.resolviendoApelacion();
      this.registrandoCaso();
      this.recienActualizadas();
      this.programarMedicion();
    });

    this.destroyRef.onDestroy(() => this.observador?.disconnect());
  }

  // ---------- rieles ----------

  private programarMedicion(): void {
    if (typeof requestAnimationFrame === 'undefined') {
      return;
    }
    requestAnimationFrame(() => this.medirRieles());
  }

  private conectarObservador(nodo: HTMLElement): void {
    if (this.observador || typeof ResizeObserver === 'undefined') {
      return;
    }
    this.observador = new ResizeObserver(() => this.programarMedicion());
    this.observador.observe(nodo);
  }

  private medirRieles(): void {
    const lienzo = this.lienzo()?.nativeElement;
    if (!lienzo) {
      return;
    }
    this.conectarObservador(lienzo);

    const nodos = new Map<string, DOMRect>();
    const base = lienzo.getBoundingClientRect();
    for (const el of Array.from(lienzo.querySelectorAll<HTMLElement>('[data-nodo]'))) {
      nodos.set(el.dataset['nodo']!, el.getBoundingClientRect());
    }
    if (nodos.size === 0) {
      this.rieles.set([]);
      return;
    }

    const lista = this.partidas();
    const enlaces: { de: string; a: string; partida: Partida | null }[] = [];
    if (lista.length === 0) {
      for (const clave of nodos.keys()) {
        const [ronda, orden] = clave.split('-').map(Number);
        const padre = `${ronda + 1}-${orden >> 1}`;
        if (nodos.has(padre)) {
          enlaces.push({ de: clave, a: padre, partida: null });
        }
      }
    } else {
      for (const p of lista) {
        if (p.siguientePartidaId !== null
          && nodos.has(`p${p.id}`) && nodos.has(`p${p.siguientePartidaId}`)) {
          enlaces.push({ de: `p${p.id}`, a: `p${p.siguientePartidaId}`, partida: p });
        }
      }
    }

    const desplazamientoX = lienzo.scrollLeft;
    const campeon = this.campeonId();
    const mios = this.misEquipos();
    const actualizadas = this.recienActualizadas();

    const rieles: Riel[] = [];
    for (const enlace of enlaces) {
      const rect = nodos.get(enlace.de)!;
      const padre = nodos.get(enlace.a)!;
      const x1 = rect.right - base.left + desplazamientoX;
      const y1 = rect.top + rect.height / 2 - base.top;
      const x2 = padre.left - base.left + desplazamientoX;
      const y2 = padre.top + padre.height / 2 - base.top;
      const xm = x1 + (x2 - x1) / 2;

      const ganador = enlace.partida?.ganadorEquipoId ?? null;
      rieles.push({
        id: enlace.de,
        d: `M ${x1} ${y1} H ${xm} V ${y2} H ${x2}`,
        campeon: campeon !== null && ganador === campeon,
        mio: campeon === null && ganador !== null && mios.includes(ganador),
        pulso: enlace.partida !== null && actualizadas.has(enlace.partida.id)
          && enlace.partida.estado === 'FINALIZADA'
      });
    }

    const geometria = JSON.stringify(rieles) + lienzo.scrollWidth + ':' + lienzo.scrollHeight;
    if (geometria === this.ultimaGeometria) {
      return;
    }
    this.ultimaGeometria = geometria;
    this.lienzoAncho.set(lienzo.scrollWidth);
    this.lienzoAlto.set(lienzo.scrollHeight);
    this.rieles.set(rieles);
  }

  // ---------- vida del bracket ----------

  private marcarActualizadas(ids: number[]): void {
    this.recienActualizadas.update((set) => new Set([...set, ...ids]));
    setTimeout(() => {
      this.recienActualizadas.update((set) => {
        const copia = new Set(set);
        for (const id of ids) {
          copia.delete(id);
        }
        return copia;
      });
    }, 2400);
  }

  private iniciarCascada(): void {
    this.generacionReciente.set(true);
    setTimeout(() => this.generacionReciente.set(false), 2600);
  }

  copiarLobby(partida: Partida, campo: 'lobby' | 'clave', valor: string): void {
    const marca = `${partida.id}-${campo}`;
    navigator.clipboard?.writeText(valor).then(
      () => {
        this.copiado.set(marca);
        setTimeout(() => {
          if (this.copiado() === marca) {
            this.copiado.set(null);
          }
        }, 1600);
      },
      () => undefined
    );
  }

  // ---------- permisos ----------

  soyCapitanDe(equipoId: number | null): boolean {
    return equipoId !== null && this.misEquipos().includes(equipoId);
  }

  puedeReportar(p: Partida): boolean {
    return this.enCurso() && p.estado === 'PENDIENTE'
      && p.equipoAId !== null && p.equipoBId !== null
      && (this.soyCapitanDe(p.equipoAId) || this.soyCapitanDe(p.equipoBId));
  }

  puedeConfirmar(p: Partida): boolean {
    if (!this.enCurso() || p.estado !== 'REPORTADA' || p.reportadoPorEquipoId === null) {
      return false;
    }
    if (this.soyCapitanDe(p.reportadoPorEquipoId)) {
      return false;
    }
    const rival = p.reportadoPorEquipoId === p.equipoAId ? p.equipoBId : p.equipoAId;
    return this.soyCapitanDe(rival);
  }

  puedeResolver(p: Partida): boolean {
    return this.enCurso() && this.esGestor() && !p.bye
      && p.equipoAId !== null && p.equipoBId !== null
      && (p.estado === 'PENDIENTE' || p.estado === 'REPORTADA' || p.estado === 'EN_DISPUTA');
  }

  puedeImpugnar(p: Partida): boolean {
    return p.estado === 'FINALIZADA' && !p.bye
      && p.equipoAId !== null && p.equipoBId !== null
      && (this.soyCapitanDe(p.equipoAId) || this.soyCapitanDe(p.equipoBId) || this.esGestor());
  }

  abrirImpugnar(p: Partida): void {
    this.impugnando.set(p.id);
    this.motivoImpugnar.set('');
    this.descripcionImpugnar.set('');
    this.evidenciaImpugnar.set('');
    this.errorImpugnar.set(null);
  }

  cerrarImpugnar(): void {
    this.impugnando.set(null);
  }

  enviarImpugnar(p: Partida): void {
    const motivo = this.motivoImpugnar().trim();
    const descripcion = this.descripcionImpugnar().trim();
    if (!motivo || !descripcion) {
      this.errorImpugnar.set('Motivo y descripción son obligatorios.');
      return;
    }
    this.errorImpugnar.set(null);
    this.impugnar.emit({
      partida: p,
      request: {
        motivo,
        descripcion,
        evidenciaUrl: this.evidenciaImpugnar().trim() || null
      }
    });
    this.impugnando.set(null);
  }

  puedeVerEvidencia(p: Partida): boolean {
    return (p.estado === 'EN_DISPUTA' || p.estado === 'FINALIZADA') && !p.bye
      && (this.soyCapitanDe(p.equipoAId) || this.soyCapitanDe(p.equipoBId) || this.esGestor());
  }

  private cargaEvidenciaToken = 0;

  toggleEvidencia(p: Partida): void {
    if (this.verEvidencia() === p.id) {
      this.verEvidencia.set(null);
      return;
    }
    this.verEvidencia.set(p.id);
    this.disputaActual.set(null);
    this.apelacionActual.set(null);
    this.evidencias.set([]);
    this.errorEvidencia.set(null);
    this.nuevaEvidenciaUrl.set('');
    this.nuevaEvidenciaDescripcion.set('');
    this.resolviendoDisputa.set(false);
    this.apelando.set(false);
    this.resolviendoApelacion.set(false);
    this.cargarDisputa(p.id);
  }

  private cargarDisputa(partidaId: number): void {
    const token = ++this.cargaEvidenciaToken;
    this.cargandoEvidencia.set(true);
    this.tournamentsService.disputasDePartida(partidaId).subscribe({
      next: (disputas: DisputaResponse[]) => {
        if (token !== this.cargaEvidenciaToken) {
          return;
        }
        const ultima = disputas.length ? disputas[disputas.length - 1] : null;
        this.disputaActual.set(ultima);
        this.cargandoEvidencia.set(false);
        if (!ultima) {
          return;
        }
        this.disputesService.listarEvidencias(ultima.id).subscribe({
          next: (lista) => {
            if (token !== this.cargaEvidenciaToken) {
              return;
            }
            this.evidencias.set(lista);
          },
          error: (err) => {
            if (token !== this.cargaEvidenciaToken) {
              return;
            }
            this.errorEvidencia.set(err?.error?.message ?? 'No se pudo cargar la evidencia.');
          }
        });

        if (ultima.estado === 'EN_APELACION' || ultima.estado === 'RESUELTA') {
          this.cargarApelacion(ultima.id);
        }
      },
      error: (err) => {
        if (token !== this.cargaEvidenciaToken) {
          return;
        }
        this.cargandoEvidencia.set(false);
        this.errorEvidencia.set(err?.error?.message ?? 'No se pudo cargar la disputa.');
      }
    });
  }

  private cargarApelacion(disputaId: number): void {
    this.disputesService.listarApelaciones(disputaId).subscribe({
      next: (lista) => {
        const activa = lista.find((a) => a.estado === 'PENDIENTE') ?? lista[lista.length - 1] ?? null;
        this.apelacionActual.set(activa);
      },
      error: (err) => this.errorApelacion.set(err?.error?.message ?? 'No se pudo cargar la apelación.')
    });
  }

  // ---------- RF-28: descansos, avances y abandonos ----------

  puedeRegistrarCaso(p: Partida): boolean {
    return this.enCurso() && this.puedeCasoEspecial() && !p.bye
      && p.equipoAId !== null && p.equipoBId !== null
      && p.estado !== 'FINALIZADA' && p.estado !== 'CANCELADA';
  }

  abrirCasoEspecial(p: Partida): void {
    this.registrandoCaso.set(p.id);
    this.tipoCaso.set('DESCANSO');
    this.justificacionCaso.set('');
    this.evidenciaCaso.set('');
    this.equipoGanadorCaso.set(null);
    this.errorCaso.set(null);
  }

  cerrarCasoEspecial(): void {
    this.registrandoCaso.set(null);
  }

  enviarCasoEspecial(p: Partida): void {
    const tipo = this.tipoCaso();
    const justificacion = this.justificacionCaso().trim();

    if (tipo === 'ABANDONO' && !justificacion) {
      this.errorCaso.set('El abandono necesita una justificación.');
      return;
    }
    if (tipo !== 'DESCANSO' && this.equipoGanadorCaso() === null) {
      this.errorCaso.set('Elegí cuál equipo avanza.');
      return;
    }

    const request: RegistrarCasoEspecialRequest = {
      tipo,
      justificacion: justificacion || null,
      evidenciaUrl: this.evidenciaCaso().trim() || null,
      equipoGanadorId: tipo === 'DESCANSO' ? null : this.equipoGanadorCaso()
    };

    this.errorCaso.set(null);
    this.enviandoCaso.set(true);
    this.tournamentsService.registrarCasoEspecial(p.id, request).subscribe({
      next: () => {
        this.enviandoCaso.set(false);
        this.registrandoCaso.set(null);
        this.casoEspecialRegistrado.emit();
      },
      error: (err) => {
        this.enviandoCaso.set(false);
        this.errorCaso.set(err?.error?.message ?? 'No se pudo registrar el caso especial.');
      }
    });
  }

  elegirArchivoEvidencia(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    entrada.value = '';
    if (!archivo) {
      return;
    }
    if (archivo.size > 5 * 1024 * 1024) {
      this.errorEvidencia.set('La imagen supera los 5 MB.');
      return;
    }
    this.subiendoArchivo.set(true);
    this.errorEvidencia.set(null);
    this.uploadsService.subirImagen(archivo).subscribe({
      next: (url) => {
        this.subiendoArchivo.set(false);
        this.nuevaEvidenciaUrl.set(url);
      },
      error: (err) => {
        this.subiendoArchivo.set(false);
        this.errorEvidencia.set(err?.error?.message ?? 'No se pudo subir la imagen.');
      }
    });
  }

  enviarEvidencia(): void {
    const disputaId = this.disputaActual()?.id;
    const url = this.nuevaEvidenciaUrl().trim();
    if (!disputaId || !url) {
      this.errorEvidencia.set('Subí una imagen o pegá un enlace antes de guardar.');
      return;
    }
    this.enviandoEvidencia.set(true);
    this.errorEvidencia.set(null);
    this.disputesService.adjuntarEvidencia(disputaId, {
      url,
      descripcion: this.nuevaEvidenciaDescripcion().trim() || null
    }).subscribe({
      next: (nueva) => {
        this.enviandoEvidencia.set(false);
        this.evidencias.update((lista) => [...lista, nueva]);
        this.nuevaEvidenciaUrl.set('');
        this.nuevaEvidenciaDescripcion.set('');
      },
      error: (err) => {
        this.enviandoEvidencia.set(false);
        this.errorEvidencia.set(err?.error?.message ?? 'No se pudo guardar la evidencia.');
      }
    });
  }

  // ---------- RF-32: resolver la disputa ----------

  puedeResolverDisputa(): boolean {
    return this.puedeCasoEspecial();
  }

  abrirResolverDisputa(): void {
    this.resolviendoDisputa.set(true);
    this.decisionResolver.set('MANTENER');
    this.justificacionResolver.set('');
    this.sancionResolver.set('');
    this.equipoGanadorResolver.set(null);
    this.errorResolucion.set(null);
  }

  cerrarResolverDisputa(): void {
    this.resolviendoDisputa.set(false);
  }

  enviarResolucion(): void {
    const disputa = this.disputaActual();
    const justificacion = this.justificacionResolver().trim();
    if (!disputa || !justificacion) {
      this.errorResolucion.set('La justificación es obligatoria.');
      return;
    }
    const decision = this.decisionResolver();
    if (decision === 'REVERTIR' && this.equipoGanadorResolver() === null) {
      this.errorResolucion.set('Elegí cuál equipo gana al revertir.');
      return;
    }
    this.enviandoResolucion.set(true);
    this.errorResolucion.set(null);
    const request: ResolverDisputaRequest = {
      decision,
      justificacion,
      sancion: this.sancionResolver().trim() || null,
      equipoGanadorId: decision === 'REVERTIR' ? this.equipoGanadorResolver() : null
    };
    this.disputesService.resolverDisputa(disputa.id, request).subscribe({
      next: (actualizada) => {
        this.enviandoResolucion.set(false);
        this.disputaActual.set(actualizada);
        this.resolviendoDisputa.set(false);
        this.disputaResuelta.emit();
      },
      error: (err) => {
        this.enviandoResolucion.set(false);
        this.errorResolucion.set(err?.error?.message ?? 'No se pudo resolver la disputa.');
      }
    });
  }

  // ---------- RF-32: apelar ----------

  puedeApelar(p: Partida): boolean {
    return this.soyCapitanDe(p.equipoAId) || this.soyCapitanDe(p.equipoBId) || this.esGestor();
  }

  abrirApelar(): void {
    this.apelando.set(true);
    this.motivoApelar.set('');
    this.errorApelacion.set(null);
  }

  cerrarApelar(): void {
    this.apelando.set(false);
  }

  enviarApelacion(): void {
    const disputa = this.disputaActual();
    const motivo = this.motivoApelar().trim();
    if (!disputa || !motivo) {
      this.errorApelacion.set('El motivo de la apelación es obligatorio.');
      return;
    }
    this.enviandoApelacion.set(true);
    this.errorApelacion.set(null);
    this.disputesService.apelar(disputa.id, { motivo }).subscribe({
      next: (apelacion) => {
        this.enviandoApelacion.set(false);
        this.apelacionActual.set(apelacion);
        this.apelando.set(false);
        this.disputaActual.update((d) => d ? { ...d, estado: 'EN_APELACION' } : d);
        this.disputaResuelta.emit();
      },
      error: (err) => {
        this.enviandoApelacion.set(false);
        this.errorApelacion.set(err?.error?.message ?? 'No se pudo registrar la apelación.');
      }
    });
  }

  // ---------- RF-32: el comisionado resuelve la apelación ----------

  puedeResolverApelacion(): boolean {
    return this.esGestor();
  }

  abrirResolverApelacion(): void {
    this.resolviendoApelacion.set(true);
    this.decisionFinalApelacion.set('');
    this.equipoGanadorApelacion.set(null);
    this.errorResolucionApelacion.set(null);
  }

  cerrarResolverApelacion(): void {
    this.resolviendoApelacion.set(false);
  }

  enviarResolucionApelacion(): void {
    const apelacion = this.apelacionActual();
    if (!apelacion) {
      return;
    }
    this.enviandoResolucionApelacion.set(true);
    this.errorResolucionApelacion.set(null);
    const request: ResolverApelacionRequest = {
      decisionFinal: this.decisionFinalApelacion().trim() || null,
      equipoGanadorId: this.equipoGanadorApelacion()
    };
    this.disputesService.resolverApelacion(apelacion.id, request).subscribe({
      next: (actualizada) => {
        this.enviandoResolucionApelacion.set(false);
        this.apelacionActual.set(actualizada);
        this.resolviendoApelacion.set(false);
        this.disputaActual.update((d) => d ? { ...d, estado: 'RESUELTA' } : d);
        this.disputaResuelta.emit();
      },
      error: (err) => {
        this.enviandoResolucionApelacion.set(false);
        this.errorResolucionApelacion.set(err?.error?.message ?? 'No se pudo resolver la apelación.');
      }
    });
  }

  formatearFecha(iso: string): string {
    return new Date(iso).toLocaleString('es-CR', {
      day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  abrirMarcador(p: Partida): void {
    this.reportando.set(p.id);
    this.marcadorA.set(p.marcadorA ?? 0);
    this.marcadorB.set(p.marcadorB ?? 0);
    this.errorLocal.set(null);
  }

  cerrarMarcador(): void {
    this.reportando.set(null);
  }

  enviar(p: Partida): void {
    if (this.marcadorA() === this.marcadorB()) {
      this.errorLocal.set('No hay empates: los marcadores deben diferir para definir un ganador.');
      return;
    }
    this.errorLocal.set(null);
    this.enviarMarcador.emit({
      partida: p,
      marcadorA: this.marcadorA(),
      marcadorB: this.marcadorB(),
      resolucion: !this.puedeReportar(p)
    });
    this.reportando.set(null);
  }

  marcadorDe(p: Partida, lado: 'A' | 'B'): string | number {
    if (p.estado !== 'FINALIZADA' && p.estado !== 'REPORTADA') {
      return '—';
    }
    const valor = lado === 'A' ? p.marcadorA : p.marcadorB;
    return valor ?? '—';
  }

  esFinalCoronada(p: Partida): boolean {
    const campeon = this.campeonId();
    return campeon !== null && p.estado === 'FINALIZADA' && p.ganadorEquipoId === campeon
      && this.partidaCulmen()?.id === p.id;
  }

  private etiquetaRonda(numero: number, total: number): string {
    switch (total - numero) {
      case 0:
        return 'Final';
      case 1:
        return 'Semifinales';
      case 2:
        return 'Cuartos de final';
      case 3:
        return 'Octavos de final';
      default:
        return `Ronda ${numero}`;
    }
  }
}
