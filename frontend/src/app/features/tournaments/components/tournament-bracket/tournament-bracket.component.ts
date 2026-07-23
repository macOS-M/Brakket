import { Component, computed, input, output, signal } from '@angular/core';

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
 */
@Component({
  selector: 'app-tournament-bracket',
  standalone: true,
  imports: [],
  templateUrl: './tournament-bracket.component.html',
  styleUrl: './tournament-bracket.component.scss'
})
export class TournamentBracketComponent {
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
