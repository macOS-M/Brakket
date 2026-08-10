import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import {
  ANCHO,
  Dominio,
  PAD,
  PuntoGrafico,
  altoInterno,
  cadenciaMs,
  crearEscalaX,
  crearEscalaY,
  decimarMinMax,
  dominioY,
  indiceMasCercano,
  pathDeArea,
  pathDeLinea,
} from './escalas';

export interface SerieGrafico {
  clave: string;
  etiqueta: string;
  /** Clase CSS, no un color: el color sale del SCSS para que siga al tema. */
  clase: string;
  puntos: PuntoGrafico[];
  area?: boolean;
}

interface Marca {
  cx: number;
  cy: number;
}

interface Trazo {
  clave: string;
  clase: string;
  linea: string;
  area: string | null;
  /** Series cortas se marcan punto a punto: con una sola muestra la línea no dibuja nada. */
  marcas: Marca[];
}

interface Tick {
  etiqueta: string;
  y: number;
}

/** Máximo de puntos antes de diezmar; por encima el path se vuelve enorme. */
const LIMITE_PUNTOS = 600;

/** Hasta acá se marca cada muestra con un círculo; más allá ensucian el trazo. */
const LIMITE_MARCAS = 60;

/**
 * RF-37: gráfico de líneas en SVG inline. Se dibuja a mano en vez de usar una
 * librería porque el tema de la app son custom properties de CSS, y un canvas
 * no las hereda.
 */
@Component({
  selector: 'app-grafico-serie',
  standalone: true,
  templateUrl: './grafico-serie.component.html',
  styleUrl: './grafico-serie.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GraficoSerieComponent {
  readonly series = input.required<SerieGrafico[]>();
  readonly alto = input(220);
  /** Dominio fijo del eje Y; el sentimiento usa -1..1 para que el cero quede al medio. */
  readonly dominioFijo = input<Dominio | undefined>(undefined);
  /** Instantes del eje X, compartidos entre los paneles para que queden alineados. */
  readonly instantes = input.required<number[]>();
  readonly intervaloSegundos = input<number | null>(null);
  readonly indiceActivo = input<number | null>(null);

  readonly indiceActivoChange = output<number | null>();

  readonly ancho = ANCHO;
  readonly pad = PAD;

  private readonly seriesVisibles = computed<SerieGrafico[]>(() =>
    this.series().map((s) => ({ ...s, puntos: decimarMinMax(s.puntos, LIMITE_PUNTOS) }))
  );

  readonly hayDatos = computed(() =>
    this.seriesVisibles().some((s) => s.puntos.some((p) => p.v !== null))
  );

  private readonly dominio = computed<Dominio>(() =>
    dominioY(
      this.seriesVisibles().flatMap((s) => s.puntos.map((p) => p.v)),
      this.dominioFijo()
    )
  );

  private readonly escalaX = computed(() => crearEscalaX(this.instantes()));
  private readonly escalaY = computed(() => crearEscalaY(this.dominio(), this.alto()));

  readonly trazos = computed<Trazo[]>(() => {
    const x = this.escalaX();
    const y = this.escalaY();
    const cadencia = cadenciaMs(this.instantes(), this.intervaloSegundos());
    const dominio = this.dominio();
    const base = dominio.min <= 0 && dominio.max >= 0 ? y(0) : y(dominio.min);

    return this.seriesVisibles().map((s) => {
      const conValor = s.puntos.filter((p) => p.v !== null);
      return {
        clave: s.clave,
        clase: s.clase,
        linea: pathDeLinea(s.puntos, x, y, cadencia),
        area: s.area ? pathDeArea(s.puntos, x, y, cadencia, base) : null,
        marcas:
          conValor.length <= LIMITE_MARCAS
            ? conValor.map((p) => ({ cx: x(p.t), cy: y(p.v as number) }))
            : [],
      };
    });
  });

  readonly ticks = computed<Tick[]>(() => {
    const { min, max } = this.dominio();
    const y = this.escalaY();
    return [0, 1, 2, 3, 4].map((k) => {
      const valor = min + ((max - min) * k) / 4;
      return { etiqueta: this.formatear(valor), y: y(valor) };
    });
  });

  readonly xGuia = computed<number | null>(() => {
    const indice = this.indiceActivo();
    const instantes = this.instantes();
    if (indice === null || indice < 0 || indice >= instantes.length) {
      return null;
    }
    return this.escalaX()(instantes[indice]);
  });

  readonly altoUtil = computed(() => altoInterno(this.alto()));

  alSenalar(evento: PointerEvent): void {
    const destino = evento.currentTarget as SVGGraphicsElement;
    const caja = destino.getBoundingClientRect();
    if (!caja.width) {
      return;
    }
    const instantes = this.instantes();
    if (instantes.length < 2) {
      this.indiceActivoChange.emit(instantes.length ? 0 : null);
      return;
    }
    // px reales -> unidades del viewBox (lineal porque preserveAspectRatio="none")
    const ux = ((evento.clientX - caja.left) / caja.width) * ANCHO;
    const t0 = Math.min(...instantes);
    const t1 = Math.max(...instantes);
    const t = t0 + ((ux - PAD.left) / (ANCHO - PAD.left - PAD.right)) * (t1 - t0);
    this.indiceActivoChange.emit(indiceMasCercano(instantes, t));
  }

  alSalir(): void {
    this.indiceActivoChange.emit(null);
  }

  alTeclear(evento: KeyboardEvent): void {
    if (evento.key !== 'ArrowLeft' && evento.key !== 'ArrowRight') {
      return;
    }
    evento.preventDefault();
    const total = this.instantes().length;
    if (!total) {
      return;
    }
    const actual = this.indiceActivo() ?? 0;
    const paso = evento.key === 'ArrowRight' ? 1 : -1;
    this.indiceActivoChange.emit(Math.min(total - 1, Math.max(0, actual + paso)));
  }

  private formatear(valor: number): string {
    if (Math.abs(valor) >= 1000) {
      return `${(valor / 1000).toFixed(1)}k`;
    }
    return Number.isInteger(valor) ? String(valor) : valor.toFixed(1);
  }
}
