import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { SponsorshipsService } from '../../services/sponsorships.service';
import { FotoInputComponent } from '../../../../shared/components/foto-input/foto-input.component';

@Component({
  selector: 'app-sponsorship-form',
  standalone: true,
  imports: [ReactiveFormsModule, FotoInputComponent],
  templateUrl: './sponsorship-form.component.html',
  styleUrl: './sponsorship-form.component.scss'
})
export class SponsorshipFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly sponsorshipsService = inject(SponsorshipsService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);
  readonly editando = signal(false);
  readonly avisoDuplicado = signal<string | null>(null);

  private patrocinadorId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    logo: [''],
    contacto: ['', [Validators.required, Validators.maxLength(180)]],
    descripcion: ['', [Validators.maxLength(500)]],
    enlaces: this.fb.nonNullable.array<string>([])
  });

  get enlaces() {
    return this.form.get('enlaces') as import('@angular/forms').FormArray;
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      return;
    }
    this.patrocinadorId = Number(idParam);
    this.editando.set(true);
    this.sponsorshipsService.obtener(this.patrocinadorId).subscribe({
      next: (p) => {
        this.form.patchValue({
          nombre: p.nombre,
          logo: p.logo ?? '',
          contacto: p.contacto,
          descripcion: p.descripcion ?? ''
        });
        p.enlaces.forEach((url) => this.enlaces.push(this.fb.nonNullable.control(url)));
      },
      error: () => this.error.set('No se pudo cargar el patrocinador a editar.')
    });
  }

  agregarEnlace(): void {
    this.enlaces.push(this.fb.nonNullable.control('', [Validators.required]));
  }

  quitarEnlace(index: number): void {
    this.enlaces.removeAt(index);
  }

  guardar(confirmarDuplicado = false): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.error.set(null);
    this.avisoDuplicado.set(null);
    const valores = this.form.getRawValue();

    const accion = this.editando()
      ? this.sponsorshipsService.editar(this.patrocinadorId!, {
        nombre: valores.nombre,
        logo: valores.logo || null,
        contacto: valores.contacto,
        descripcion: valores.descripcion || null,
        enlaces: valores.enlaces
      })
      : this.sponsorshipsService.crear({
        nombre: valores.nombre,
        logo: valores.logo || null,
        contacto: valores.contacto,
        descripcion: valores.descripcion || null,
        enlaces: valores.enlaces,
        confirmarDuplicado
      });

    accion.subscribe({
      next: () => this.router.navigate(['/sponsorships']),
      error: (err) => {
        this.guardando.set(false);
        const mensaje = err?.error?.message ?? 'No se pudo guardar el patrocinador.';
        if (!this.editando() && err?.status === 409 && !confirmarDuplicado) {
          this.avisoDuplicado.set(mensaje);
        } else {
          this.error.set(mensaje);
        }
      }
    });
  }

  confirmarYGuardar(): void {
    this.guardar(true);
  }

  cancelar(): void {
    this.router.navigate(['/sponsorships']);
  }
}
