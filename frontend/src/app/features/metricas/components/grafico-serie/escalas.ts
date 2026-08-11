/**
 * RF-37: matemática del gráfico de series. Son funciones puras a propósito:
 * el componente solo las orquesta, y así el escalado se puede probar sin DOM.
 *
 * Ojo con las fechas: la API devuelve LocalDateTime sin zona ("2026-07-12T20:00:00")
 * y el navegador lo interpreta como hora local, que es justo lo que queremos.
 * No agregar "Z" ni un offset al parsear.
 */

export const ANCHO = 1000;
export const PAD = { top: 12, right: 12, bottom: 4, left: 8 };

export interface PuntoGrafico {
  t: number;
  v: number | null;
}

export interface Dominio {
  min: number;
  max: number;
}

export function anchoInterno(): number {
  return ANCHO - PAD.left - PAD.right;
}

export function altoInterno(alto: number): number {
  return alto - PAD.top - PAD.bottom;
}

/** Techo "lindo": 1, 2 o 5 por una potencia de 10, justo por encima del máximo. */
export function techoLindo(max: number): number {
  if (!isFinite(max) || max <= 0) {
    return 1;
  }
  const base = Math.pow(10, Math.floor(Math.log10(max)));
  const norm = max / base;
  const paso = norm <= 1 ? 1 : norm <= 2 ? 2 : norm <= 5 ? 5 : 10;
  return paso * base;
}

/** Los conteos anclan en 0; un dominio fijo (sentimiento) se respeta tal cual. */
export function dominioY(valores: (number | null)[], fijo?: Dominio): Dominio {
  if (fijo) {
    return fijo;
  }
  const reales = valores.filter((v): v is number => v !== null && isFinite(v));
  if (!reales.length) {
    return { min: 0, max: 1 };
  }
  const max = techoLindo(Math.max(...reales));
  return { min: 0, max: max === 0 ? 1 : max };
}

export function crearEscalaX(instantes: number[]): (t: number) => number {
  const interno = anchoInterno();
  if (instantes.length <= 1) {
    return () => PAD.left + interno / 2;
  }
  const t0 = Math.min(...instantes);
  const t1 = Math.max(...instantes);
  const span = Math.max(1, t1 - t0);
  return (t: number) => PAD.left + ((t - t0) / span) * interno;
}

/** En SVG la Y crece hacia abajo, así que la escala va invertida. */
export function crearEscalaY(dominio: Dominio, alto: number): (v: number) => number {
  const interno = altoInterno(alto);
  const min = dominio.min;
  const max = dominio.max === dominio.min ? dominio.min + 1 : dominio.max;
  return (v: number) => PAD.top + interno - ((v - min) / (max - min)) * interno;
}

/**
 * Cadencia esperada entre muestras. Se usa la que informa el backend y, si no
 * viene, la mediana de los saltos observados.
 */
export function cadenciaMs(instantes: number[], intervaloSegundos: number | null): number {
  if (intervaloSegundos && intervaloSegundos > 0) {
    return intervaloSegundos * 1000;
  }
  if (instantes.length < 2) {
    return 60_000;
  }
  const saltos = instantes.slice(1).map((t, i) => t - instantes[i]).sort((a, b) => a - b);
  return saltos[Math.floor(saltos.length / 2)] || 60_000;
}

/**
 * Un corte del muestreo no se dibuja como una recta que lo atraviesa: eso sería
 * inventar datos. Se abre un trazo nuevo cuando el punto es nulo o cuando el
 * salto supera 2.5 veces la cadencia.
 */
function hayCorte(puntos: PuntoGrafico[], i: number, cadencia: number): boolean {
  if (i === 0) {
    return true;
  }
  const anterior = puntos[i - 1];
  return anterior.v === null || puntos[i].t - anterior.t > 2.5 * cadencia;
}

export function pathDeLinea(
  puntos: PuntoGrafico[],
  x: (t: number) => number,
  y: (v: number) => number,
  cadencia: number
): string {
  let d = '';
  for (let i = 0; i < puntos.length; i++) {
    const punto = puntos[i];
    if (punto.v === null) {
      continue;
    }
    const comando = hayCorte(puntos, i, cadencia) ? 'M' : 'L';
    d += `${comando} ${x(punto.t).toFixed(1)} ${y(punto.v).toFixed(1)} `;
  }
  return d.trim();
}

/** Igual que la línea, pero cada trazo se cierra contra la línea base. */
export function pathDeArea(
  puntos: PuntoGrafico[],
  x: (t: number) => number,
  y: (v: number) => number,
  cadencia: number,
  base: number
): string {
  let d = '';
  let inicio: PuntoGrafico | null = null;
  let ultimo: PuntoGrafico | null = null;

  const cerrar = () => {
    if (inicio && ultimo) {
      d += `L ${x(ultimo.t).toFixed(1)} ${base.toFixed(1)} `;
      d += `L ${x(inicio.t).toFixed(1)} ${base.toFixed(1)} Z `;
    }
    inicio = null;
    ultimo = null;
  };

  for (let i = 0; i < puntos.length; i++) {
    const punto = puntos[i];
    if (punto.v === null) {
      cerrar();
      continue;
    }
    if (hayCorte(puntos, i, cadencia)) {
      cerrar();
      d += `M ${x(punto.t).toFixed(1)} ${y(punto.v).toFixed(1)} `;
      inicio = punto;
    } else {
      d += `L ${x(punto.t).toFixed(1)} ${y(punto.v).toFixed(1)} `;
    }
    ultimo = punto;
  }
  cerrar();
  return d.trim();
}

/**
 * Reduce la serie conservando mínimo y máximo de cada cubeta. Con agrupación
 * cruda una transmisión larga trae miles de puntos, y un promedio se comería
 * justo los picos, que es lo que hay que ver.
 */
export function decimarMinMax(puntos: PuntoGrafico[], cubetas: number): PuntoGrafico[] {
  if (puntos.length <= cubetas || cubetas < 1) {
    return puntos;
  }
  const tamano = Math.ceil(puntos.length / cubetas);
  const salida: PuntoGrafico[] = [];

  for (let inicio = 0; inicio < puntos.length; inicio += tamano) {
    const cubeta = puntos.slice(inicio, inicio + tamano);
    const conValor = cubeta.filter((p): p is { t: number; v: number } => p.v !== null);
    const elegidos: PuntoGrafico[] = [];

    const nulo = cubeta.find((p) => p.v === null);
    if (nulo) {
      elegidos.push(nulo);
    }
    if (conValor.length) {
      const minimo = conValor.reduce((a, b) => (b.v < a.v ? b : a));
      const maximo = conValor.reduce((a, b) => (b.v > a.v ? b : a));
      elegidos.push(minimo);
      if (maximo !== minimo) {
        elegidos.push(maximo);
      }
    }
    elegidos.sort((a, b) => a.t - b.t);
    salida.push(...elegidos);
  }
  return salida;
}

/** Índice de la muestra más cercana a un instante, por búsqueda binaria. */
export function indiceMasCercano(instantes: number[], t: number): number {
  if (!instantes.length) {
    return -1;
  }
  let bajo = 0;
  let alto = instantes.length - 1;
  while (alto - bajo > 1) {
    const medio = (bajo + alto) >> 1;
    if (instantes[medio] <= t) {
      bajo = medio;
    } else {
      alto = medio;
    }
  }
  return Math.abs(instantes[bajo] - t) <= Math.abs(instantes[alto] - t) ? bajo : alto;
}

/** "2026-07-12T20:00:00" -> epoch ms en hora local. */
export function aEpoch(instante: string): number {
  return new Date(instante).getTime();
}
