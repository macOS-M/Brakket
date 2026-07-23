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

/**
 * La llave del torneo (RF-26/27), estilo Challenger Mode: columnas por
 * ronda. Antes de iniciar muestra los cruces "Por decidir" según el cupo;
 * en curso muestra lobby, marcadores y las acciones que correspondan al
 * rol (reportar / confirmar / rechazar / resolver).
 *
 * Los rieles entre rondas se dibujan en un SVG medido sobre el DOM real
 * (las tarjetas cambian de alto al abrir formularios). El camino del
 * campeón se pinta en oro, el de tus equipos en acento, y cuando una
 * partida se finaliza el riel hacia el siguiente cruce pulsa una vez.
 */
@Component({
  selector: 'app-tournament-bracket',
  standalone: true,
  imports: [],
  templateUrl: './tournament-bracket.component.html',
  styleUrl: './tournament-bracket.component.scss'
})
export class TournamentBracketComponent {
  private readonly destroyRef = inject(DestroyRef);

  readonly partidas = input.required<Partida[]>();
  /** Cupo del torneo: dibuja la llave tentativa antes de generarse. */
  readonly maxEquipos = input.required<number>();
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

  /** Equipo campeón: el ganador de la final, si ya existe. */
  readonly campeonId = computed<number | null>(() => {
    const lista = this.partidas();
    if (lista.length === 0) {
      return null;
    }
    const total = Math.max(...lista.map((p) => p.ronda));
    const final = lista.find((p) => p.ronda === total);
    return final?.estado === 'FINALIZADA' ? final.ganadorEquipoId : null;
  });

  /** Índice global de tarjeta (orden ronda a ronda) para la cascada. */
  readonly indicesCascada = computed<Map<number, number>>(() => {
    const indices = new Map<number, number>();
    let i = 0;
    for (const ronda of this.rondas()) {
      for (const p of ronda.partidas) {
        indices.set(p.id, i++);
      }
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
   * Mide cada tarjeta contra el lienzo y traza el codo hasta su cruce
   * padre (ronda r, orden o → ronda r+1, orden o>>1). Vale tanto para la
   * llave real como para la tentativa: ambas marcan data-nodo="r-o".
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

    const desplazamientoX = lienzo.scrollLeft;
    const porId = new Map(this.partidas().map((p) => [`${p.ronda}-${p.orden}`, p]));
    const campeon = this.campeonId();
    const mios = this.misEquipos();
    const actualizadas = this.recienActualizadas();

    const rieles: Riel[] = [];
    for (const [clave, rect] of nodos) {
      const [ronda, orden] = clave.split('-').map(Number);
      const padre = nodos.get(`${ronda + 1}-${orden >> 1}`);
      if (!padre) {
        continue;
      }
      const x1 = rect.right - base.left + desplazamientoX;
      const y1 = rect.top + rect.height / 2 - base.top;
      const x2 = padre.left - base.left + desplazamientoX;
      const y2 = padre.top + padre.height / 2 - base.top;
      const xm = x1 + (x2 - x1) / 2;

      const partida = porId.get(clave);
      const ganador = partida?.ganadorEquipoId ?? null;
      rieles.push({
        id: clave,
        d: `M ${x1} ${y1} H ${xm} V ${y2} H ${x2}`,
        campeon: campeon !== null && ganador === campeon,
        mio: campeon === null && ganador !== null && mios.includes(ganador),
        pulso: partida !== undefined && actualizadas.has(partida.id)
          && partida.estado === 'FINALIZADA'
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
      this.errorLocal.set('En eliminación directa no hay empates: los marcadores deben diferir.');
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
      && p.ronda === Math.max(...this.partidas().map((x) => x.ronda));
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
