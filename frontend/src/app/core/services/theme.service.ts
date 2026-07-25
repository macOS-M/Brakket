import { Injectable, effect, signal } from '@angular/core';

export type Tema = 'claro' | 'oscuro' | 'sistema';

const CLAVE = 'brakket.tema';

/**
 * Gestiona el tema visual.
 *
 * Tres estados: claro, oscuro y "sistema" (sigue a prefers-color-scheme).
 * Sistema es el valor por defecto, porque el contexto de uso varia por rol:
 * los organizadores trabajan de dia y los jugadores de noche.
 *
 * El tema se aplica escribiendo data-theme en <html>; cuando el valor es
 * "sistema" se quita el atributo y manda la media query de _theme.scss.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly tema = signal<Tema>(this.leerPreferencia());

  /** Tema efectivo, ya resuelto contra la preferencia del sistema. */
  readonly efectivo = signal<'claro' | 'oscuro'>('oscuro');

  private readonly consultaSistema =
    typeof window !== 'undefined' && window.matchMedia
      ? window.matchMedia('(prefers-color-scheme: light)')
      : null;

  constructor() {
    // Si el usuario esta en "sistema", hay que reaccionar cuando cambia
    // la preferencia del SO sin recargar la pagina.
    this.consultaSistema?.addEventListener('change', () => {
      if (this.tema() === 'sistema') {
        this.aplicar(this.tema());
      }
    });

    effect(() => {
      const valor = this.tema();
      this.aplicar(valor);
      try {
        localStorage.setItem(CLAVE, valor);
      } catch {
        // Modo privado o almacenamiento bloqueado: el tema igual funciona
        // en esta sesion, solo no se recuerda.
      }
    });
  }

  alternar(): void {
    // Cicla entre los tres estados en el orden mas predecible para el usuario.
    const siguiente: Record<Tema, Tema> = {
      sistema: 'claro',
      claro: 'oscuro',
      oscuro: 'sistema'
    };
    this.tema.set(siguiente[this.tema()]);
  }

  private aplicar(valor: Tema): void {
    const raiz = document.documentElement;
    if (valor === 'sistema') {
      raiz.removeAttribute('data-theme');
      this.efectivo.set(this.consultaSistema?.matches ? 'claro' : 'oscuro');
    } else {
      raiz.setAttribute('data-theme', valor === 'claro' ? 'light' : 'dark');
      this.efectivo.set(valor);
    }
    this.sincronizarColorDeBarra();
  }

  /** Mantiene la barra del navegador en movil alineada con el tema. */
  private sincronizarColorDeBarra(): void {
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) {
      meta.setAttribute('content', this.efectivo() === 'claro' ? '#f8fafc' : '#0b1120');
    }
  }

  private leerPreferencia(): Tema {
    try {
      const guardado = localStorage.getItem(CLAVE);
      if (guardado === 'claro' || guardado === 'oscuro' || guardado === 'sistema') {
        return guardado;
      }
    } catch {
      // Sin acceso a localStorage: se cae al valor por defecto.
    }
    return 'sistema';
  }
}
