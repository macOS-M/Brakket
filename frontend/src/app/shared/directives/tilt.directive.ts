import { Directive, ElementRef, OnDestroy, inject } from '@angular/core';

/**
 * Tilt 3D sutil hacia el cursor + coordenadas para el brillo que lo sigue.
 * Escribe --tilt-x / --tilt-y (grados) y --gx / --gy (px locales) en el host;
 * el CSS del componente decide qué hacer con ellas. Inerte con puntero
 * grueso (táctil) o con prefers-reduced-motion: ahí no registra listeners.
 */
@Directive({ selector: '[appTilt]', standalone: true })
export class TiltDirective implements OnDestroy {
  /** Inclinación máxima en grados: presencia, no mareo. */
  private static readonly MAX_GRADOS = 3.5;

  private readonly host = inject(ElementRef<HTMLElement>).nativeElement;
  private readonly activo: boolean;
  private cuadro: number | null = null;

  private readonly alMover = (evento: PointerEvent): void => {
    if (this.cuadro !== null) {
      return;
    }
    this.cuadro = requestAnimationFrame(() => {
      this.cuadro = null;
      const rect = this.host.getBoundingClientRect();
      const x = (evento.clientX - rect.left) / rect.width;
      const y = (evento.clientY - rect.top) / rect.height;
      const max = TiltDirective.MAX_GRADOS;
      this.host.style.setProperty('--tilt-x', `${((0.5 - y) * 2 * max).toFixed(2)}deg`);
      this.host.style.setProperty('--tilt-y', `${((x - 0.5) * 2 * max).toFixed(2)}deg`);
      this.host.style.setProperty('--gx', `${(evento.clientX - rect.left).toFixed(0)}px`);
      this.host.style.setProperty('--gy', `${(evento.clientY - rect.top).toFixed(0)}px`);
    });
  };

  private readonly alSalir = (): void => {
    if (this.cuadro !== null) {
      cancelAnimationFrame(this.cuadro);
      this.cuadro = null;
    }
    this.host.style.setProperty('--tilt-x', '0deg');
    this.host.style.setProperty('--tilt-y', '0deg');
  };

  constructor() {
    this.activo = window.matchMedia('(pointer: fine)').matches
      && !window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (this.activo) {
      this.host.addEventListener('pointermove', this.alMover);
      this.host.addEventListener('pointerleave', this.alSalir);
    }
  }

  ngOnDestroy(): void {
    if (this.activo) {
      this.host.removeEventListener('pointermove', this.alMover);
      this.host.removeEventListener('pointerleave', this.alSalir);
      this.alSalir();
    }
  }
}
