import { Component, forwardRef, inject, input, signal } from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';

import { UploadsService } from '../../services/uploads.service';

/** Imagen elegida pendiente de encuadre. */
interface Recorte {
  url: string;
  anchoNatural: number;
  altoNatural: number;
}

/**
 * Control de imagen para formularios reactivos: subir un archivo directo
 * (se guarda en el backend y el control recibe la URL) o pegar una URL
 * externa. Se usa con formControlName en todo formulario que pida foto.
 *
 * Al elegir un archivo se abre un recortador con marco fijo (cuadrado
 * para logos, apaisado para banners): la imagen se arrastra y acerca
 * dentro del marco y lo que queda afuera se corta antes de subir, así
 * ninguna foto llega con la proporción equivocada.
 */
@Component({
  selector: 'app-foto-input',
  standalone: true,
  templateUrl: './foto-input.component.html',
  styleUrl: './foto-input.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FotoInputComponent),
      multi: true
    }
  ]
})
export class FotoInputComponent implements ControlValueAccessor {
  private readonly uploads = inject(UploadsService);

  /** 'cuadrada' para logos/avatares; 'banner' para portadas anchas. */
  readonly forma = input<'cuadrada' | 'banner'>('cuadrada');
  /** Nombra el control para el lector de pantalla ("Logo", "Banner"…). */
  readonly etiqueta = input.required<string>();

  readonly valor = signal<string>('');
  readonly subiendo = signal(false);
  readonly errorSubida = signal<string | null>(null);
  readonly deshabilitado = signal(false);

  // ---------- recortador ----------
  readonly recorte = signal<Recorte | null>(null);
  readonly zoom = signal(1);
  readonly ox = signal(0);
  readonly oy = signal(0);

  private arrastrando = false;
  private ultimoX = 0;
  private ultimoY = 0;

  private alCambiar: (valor: string) => void = () => undefined;
  private alTocar: () => void = () => undefined;

  /** Marco visible del recortador (px lógicos, fijos para la matemática). */
  get marco(): { w: number; h: number } {
    return this.forma() === 'banner' ? { w: 400, h: 100 } : { w: 300, h: 300 };
  }

  /** Resolución final que se sube al backend. */
  private get destino(): { w: number; h: number } {
    return this.forma() === 'banner' ? { w: 1600, h: 400 } : { w: 512, h: 512 };
  }

  /** Escala que hace que la imagen cubra el marco por completo (zoom 1). */
  private get escalaBase(): number {
    const r = this.recorte();
    if (!r) {
      return 1;
    }
    return Math.max(this.marco.w / r.anchoNatural, this.marco.h / r.altoNatural);
  }

  /** Escala efectiva actual. */
  private get escala(): number {
    return this.escalaBase * this.zoom();
  }

  /** Ancho en pantalla de la imagen dentro del recortador. */
  anchoImagen(): number {
    const r = this.recorte();
    return r ? r.anchoNatural * this.escala : 0;
  }

  writeValue(valor: string | null): void {
    this.valor.set(valor ?? '');
  }

  registerOnChange(fn: (valor: string) => void): void {
    this.alCambiar = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.alTocar = fn;
  }

  setDisabledState(deshabilitado: boolean): void {
    this.deshabilitado.set(deshabilitado);
  }

  alEscribirUrl(url: string): void {
    this.valor.set(url);
    this.errorSubida.set(null);
    this.alCambiar(url);
    this.alTocar();
  }

  alElegirArchivo(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    // Permite volver a elegir el mismo archivo tras un error.
    entrada.value = '';
    if (!archivo) {
      return;
    }
    if (archivo.size > 5 * 1024 * 1024) {
      this.errorSubida.set('La imagen supera los 5 MB.');
      return;
    }
    this.errorSubida.set(null);

    const url = URL.createObjectURL(archivo);
    const imagen = new Image();
    imagen.onload = () => {
      this.recorte.set({
        url,
        anchoNatural: imagen.naturalWidth,
        altoNatural: imagen.naturalHeight
      });
      this.zoom.set(1);
      // Centrada de entrada: el usuario ajusta desde ahí.
      this.ox.set((this.marco.w - imagen.naturalWidth * this.escalaBase) / 2);
      this.oy.set((this.marco.h - imagen.naturalHeight * this.escalaBase) / 2);
    };
    imagen.onerror = () => {
      URL.revokeObjectURL(url);
      this.errorSubida.set('No se pudo leer la imagen.');
    };
    imagen.src = url;
  }

  // ---------- interacción del recortador ----------

  iniciarArrastre(evento: PointerEvent): void {
    this.arrastrando = true;
    this.ultimoX = evento.clientX;
    this.ultimoY = evento.clientY;
    (evento.target as HTMLElement).setPointerCapture?.(evento.pointerId);
  }

  arrastrar(evento: PointerEvent): void {
    if (!this.arrastrando) {
      return;
    }
    evento.preventDefault();
    this.ox.set(this.ox() + (evento.clientX - this.ultimoX));
    this.oy.set(this.oy() + (evento.clientY - this.ultimoY));
    this.ultimoX = evento.clientX;
    this.ultimoY = evento.clientY;
    this.encuadrar();
  }

  soltarArrastre(): void {
    this.arrastrando = false;
  }

  alZoom(nuevoZoom: number): void {
    const r = this.recorte();
    if (!r) {
      return;
    }
    // El centro del marco se mantiene fijo al acercar o alejar.
    const factor = (this.escalaBase * nuevoZoom) / this.escala;
    this.ox.set(this.marco.w / 2 - (this.marco.w / 2 - this.ox()) * factor);
    this.oy.set(this.marco.h / 2 - (this.marco.h / 2 - this.oy()) * factor);
    this.zoom.set(nuevoZoom);
    this.encuadrar();
  }

  /** La imagen jamás deja huecos: el marco siempre queda cubierto. */
  private encuadrar(): void {
    const r = this.recorte();
    if (!r) {
      return;
    }
    const anchoImg = r.anchoNatural * this.escala;
    const altoImg = r.altoNatural * this.escala;
    this.ox.set(Math.min(0, Math.max(this.marco.w - anchoImg, this.ox())));
    this.oy.set(Math.min(0, Math.max(this.marco.h - altoImg, this.oy())));
  }

  confirmarRecorte(): void {
    const r = this.recorte();
    if (!r || this.subiendo()) {
      return;
    }
    const lienzo = document.createElement('canvas');
    lienzo.width = this.destino.w;
    lienzo.height = this.destino.h;
    const ctx = lienzo.getContext('2d');
    if (!ctx) {
      this.errorSubida.set('El navegador no permitió recortar la imagen.');
      return;
    }
    const imagen = new Image();
    imagen.onload = () => {
      const s = this.escala;
      ctx.drawImage(
        imagen,
        -this.ox() / s,
        -this.oy() / s,
        this.marco.w / s,
        this.marco.h / s,
        0,
        0,
        this.destino.w,
        this.destino.h
      );
      lienzo.toBlob(
        (blob) => {
          if (!blob) {
            this.errorSubida.set('No se pudo generar el recorte.');
            return;
          }
          this.subiendo.set(true);
          const archivo = new File([blob], 'recorte.webp', { type: 'image/webp' });
          this.uploads.subirImagen(archivo).subscribe({
            next: (url) => {
              this.subiendo.set(false);
              this.cerrarRecorte();
              this.valor.set(url);
              this.alCambiar(url);
              this.alTocar();
            },
            error: (err) => {
              this.subiendo.set(false);
              this.errorSubida.set(err?.error?.message ?? 'No se pudo subir la imagen.');
            }
          });
        },
        'image/webp',
        0.9
      );
    };
    imagen.src = r.url;
  }

  cancelarRecorte(): void {
    if (this.subiendo()) {
      return;
    }
    this.cerrarRecorte();
  }

  private cerrarRecorte(): void {
    const r = this.recorte();
    if (r) {
      URL.revokeObjectURL(r.url);
    }
    this.recorte.set(null);
    this.arrastrando = false;
  }

  quitar(): void {
    this.valor.set('');
    this.errorSubida.set(null);
    this.alCambiar('');
    this.alTocar();
  }
}
