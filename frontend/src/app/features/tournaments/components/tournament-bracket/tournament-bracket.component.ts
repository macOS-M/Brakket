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

  readonly partidas = input.required<Partida[]>();
  /** Cupo del torneo: dibuja la llave tentativa antes de generarse. */
  readonly maxEquipos = input.required<number>();
  /** Código o nombre del formato; define la vista. */
  readonly formato = input<string>('ELIMINACION_DIRECTA');
  /** Campeón según el torneo (para coronar tabla o final). */
  readonly campeonEquipoId = input<number | null>(null);
  readonly misEquipos = input<number[]>([]);
  readonly esGestor = input(false);
  readonly enCurso = input(false);
  readonly ocupado = input(false);

  readonly enviarMarcador = output<MarcadorEvent>();
  readonly confirmar = output<Partida>();
  readonly rechazar = output<Partida>();

  /** Partida con el formulario de marcador abierto. */
  readonly reportando = signal<number | null>(null);
  readonly marcadorA = signal(0);
  readonly marcadorB = signal(0);
  readonly errorLocal = signal<string | null>(null);

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
    // Mismo desempate final que TablaPosiciones del backend (id de equipo):
    // si difieren, la corona podría dibujarse en otra fila de la tabla.
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
      return null; // la corona vive en la tabla, no en una partida
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
    // Detecta transiciones de estado (para pulso/flash) y la generación
    // en vivo de la llave (para la cascada). Corre antes del repintado.
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

    // Re-mide los rieles cuando cambian los datos que alteran el layout.
    effect(() => {
      this.partidas();
      this.maxEquipos();
      this.reportando();
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

  /**
   * Mide cada tarjeta contra el lienzo y traza el codo hasta su cruce de
   * avance. En la llave real el enlace es el de verdad (siguientePartidaId,
   * data-nodo="p{id}"); en la tentativa rige el árbol binario (r-o).
   */
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
