import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { EspaciosPublicitariosService } from '../../services/espacios-publicitarios.service';
import { PatrociniosService } from '../../services/patrocinios.service';
import { UploadService } from '../../../../core/services/upload.service';
import { UBICACIONES_ESPACIO, UbicacionEspacio } from '../../../../models/espacio-publicitario.model';
import { EtiquetaPipe } from '../../../../shared/pipes/etiqueta.pipe';

@Component({
  selector: 'app-espacio-form',
  standalone: true,
  imports: [ReactiveFormsModule, EtiquetaPipe],
  templateUrl: './espacio-form.component.html',
  styleUrl: './espacio-form.component.scss'
})
export class EspacioFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly espaciosService = inject(EspaciosPublicitariosService);
  private readonly patrociniosService = inject(PatrociniosService);
  private readonly uploadService = inject(UploadService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly ubicaciones = signal<readonly UbicacionEspacio[]>(UBICACIONES_ESPACIO);
  readonly guardando = signal(false);
  readonly cargando = signal(false);
  readonly subiendoImagen = signal(false);
  readonly error = signal<string | null>(null);
  readonly previewUrl = signal<string | null>(null);
  readonly editando = signal(false);
  private readonly TAMANO_MAXIMO_BYTES = 2 * 1024 * 1024; // 2MB

  private patrocinioId!: number;
  private espacioId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    ubicacion: ['TORNEO_CABECERA', Validators.required],
    imagenUrl: ['', Validators.required],
    enlaceUrl: ['']
  });

  ngOnInit(): void {
    this.patrocinioId = Number(this.route.snapshot.paramMap.get('patrocinioId'));
    const idParam = this.route.snapshot.paramMap.get('id');
    this.espacioId = idParam ? Number(idParam) : null;
    this.editando.set(this.espacioId !== null);

    // Filtra el catálogo según el alcance real del patrocinio (liga/torneo),
    // igual en crear y en editar — el alcance no puede cambiar desde acá.
    this.patrociniosService.obtener(this.patrocinioId).subscribe({
      next: (patrocinio) => {
        const permitidas = this.ubicacionesPermitidasPara(patrocinio);
        this.ubicaciones.set(permitidas);
        // Solo se pisa el default en modo creación: en edición, el valor real
        // ya lo trae el propio espacio (se carga aparte, más abajo).
        if (!this.editando() && !permitidas.includes(this.form.controls.ubicacion.value as UbicacionEspacio)) {
          this.form.controls.ubicacion.setValue(permitidas[0]);
        }
      },
      error: () => {
        // No se bloquea el formulario: el backend igual valida el alcance real al guardar.
      }
    });

    if (this.espacioId !== null) {
      this.cargando.set(true);
      // No existe GET /api/espacios/{id}: se reusa el listado por patrocinio
      // y se busca el id acá, para no agregar un endpoint nuevo a esta hora.
      this.espaciosService.listarPorPatrocinio(this.patrocinioId).subscribe({
        next: (espacios) => {
          const existente = espacios.find((e) => e.id === this.espacioId);
          if (existente) {
            this.form.patchValue({
              ubicacion: existente.ubicacion,
              imagenUrl: existente.imagenUrl,
              enlaceUrl: existente.enlaceUrl ?? ''
            });
            this.previewUrl.set(existente.imagenUrl);
          } else {
            this.error.set('No se encontró el espacio publicitario a editar.');
          }
          this.cargando.set(false);
        },
        error: () => {
          this.error.set('No se pudo cargar el espacio publicitario a editar.');
          this.cargando.set(false);
        }
      });
    }
  }

  private ubicacionesPermitidasPara(patrocinio: { ligaId: number | null; torneoId: number | null }): UbicacionEspacio[] {
    if (patrocinio.ligaId != null) return ['LIGA_CABECERA'];
    if (patrocinio.torneoId != null) return ['TORNEO_CABECERA', 'TRANSMISION_INFERIOR'];
    return [...UBICACIONES_ESPACIO];
  }

  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (!archivo) {
      return;
    }

    if (archivo.size > this.TAMANO_MAXIMO_BYTES) {
      this.error.set('La imagen no puede superar los 2MB. Elegí un archivo más liviano.');
      input.value = '';
      return;
    }

    this.subiendoImagen.set(true);
    this.error.set(null);

    this.uploadService.subirImagen(archivo).subscribe({
      next: (res) => {
        this.form.patchValue({ imagenUrl: res.url });
        this.previewUrl.set(res.url);
        this.subiendoImagen.set(false);
      },
      error: () => {
        this.error.set('No se pudo subir la imagen. Intenta de nuevo.');
        this.subiendoImagen.set(false);
      }
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.error.set(null);
    const valores = this.form.getRawValue();

    const accion = this.editando()
      ? this.espaciosService.editar(this.espacioId!, {
        ubicacion: valores.ubicacion,
        imagenUrl: valores.imagenUrl,
        enlaceUrl: valores.enlaceUrl || null
      })
      : this.espaciosService.crear({
        patrocinioId: this.patrocinioId,
        ubicacion: valores.ubicacion,
        imagenUrl: valores.imagenUrl,
        enlaceUrl: valores.enlaceUrl || null
      });

    accion.subscribe({
      next: () => this.router.navigate(['/sponsorships/asociaciones', this.patrocinioId, 'espacios']),
      error: (err) => {
        this.guardando.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo guardar el espacio publicitario.');
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/sponsorships/asociaciones', this.patrocinioId, 'espacios']);
  }
}
