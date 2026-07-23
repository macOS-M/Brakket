import { Component, forwardRef, inject, input, signal } from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';

import { UploadsService } from '../../services/uploads.service';

/**
 * Control de imagen para formularios reactivos: subir un archivo directo
 * (se guarda en el backend y el control recibe la URL) o pegar una URL
 * externa. Se usa con formControlName en todo formulario que pida foto.
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

  private alCambiar: (valor: string) => void = () => undefined;
  private alTocar: () => void = () => undefined;

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
    this.subiendo.set(true);
    this.errorSubida.set(null);
    this.uploads.subirImagen(archivo).subscribe({
      next: (url) => {
        this.subiendo.set(false);
        this.valor.set(url);
        this.alCambiar(url);
        this.alTocar();
      },
      error: (err) => {
        this.subiendo.set(false);
        this.errorSubida.set(err?.error?.message ?? 'No se pudo subir la imagen.');
      }
    });
  }

  quitar(): void {
    this.valor.set('');
    this.errorSubida.set(null);
    this.alCambiar('');
    this.alTocar();
  }
}
