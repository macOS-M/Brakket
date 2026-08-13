import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { FechaInputComponent } from '../../../../shared/components/fecha-input/fecha-input.component';
import { FiltrosReporte, ReporteResponse, TipoReporte } from '../../../../models/reporte.model';
import { Patrocinador } from '../../../../models/patrocinador.model';
import { Torneo } from '../../../../models/tournament.model';
import { SponsorshipsService } from '../../../sponsorships/services/sponsorships.service';
import { TournamentsService } from '../../../tournaments/services/tournaments.service';
import { ReportsService } from '../../services/reports.service';

@Component({
  selector: 'app-reports-view',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FechaInputComponent],
  templateUrl: './reports-view.component.html',
  styleUrl: './reports-view.component.scss'
})
export class ReportsViewComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly reports = inject(ReportsService);
  private readonly sponsorshipsService = inject(SponsorshipsService);
  private readonly tournamentsService = inject(TournamentsService);

  readonly resultado = signal<ReporteResponse | null>(null);
  readonly cargando = signal(false);
  readonly descargando = signal(false);
  readonly error = signal('');

  readonly torneos = signal<Torneo[]>([]);
  readonly patrocinadores = signal<Patrocinador[]>([]);

  readonly form = this.fb.group({
    tipo: this.fb.nonNullable.control<TipoReporte>('COMPETENCIA', Validators.required),
    torneoId: this.fb.control<number | null>(null),
    patrocinadorId: this.fb.control<number | null>(null),
    desde: this.fb.nonNullable.control(''),
    hasta: this.fb.nonNullable.control('')
  });

  ngOnInit(): void {
    // Mismas fuentes que usa el formulario de asociación de patrocinios (RF-42),
    // pero sin el filtro a solo ACTIVO que tiene ese formulario: acá sí interesa
    // poder reportar sobre patrocinios ya vencidos o marcas dadas de baja.
    this.tournamentsService.listar().subscribe((data) => this.torneos.set(data));
    this.sponsorshipsService.listar().subscribe((data) => this.patrocinadores.set(data));
  }

  generar(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    if (raw.desde && raw.hasta && raw.desde > raw.hasta) {
      this.error.set('La fecha inicial no puede ser posterior a la fecha final.');
      return;
    }
    this.cargando.set(true);
    this.error.set('');
    this.resultado.set(null);
    this.reports.generar(this.filtrosActuales())
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: r => this.resultado.set(r),
        error: e => this.error.set(e?.error?.message ?? 'No fue posible generar el reporte.')
      });
  }

  descargarPdf(): void {
    if (!this.resultado()) return;
    this.descargando.set(true);
    this.reports.generarPdf(this.filtrosActuales())
      .pipe(finalize(() => this.descargando.set(false)))
      .subscribe({
        next: blob => this.disparaDescarga(blob),
        error: () => this.error.set('No fue posible descargar el PDF.')
      });
  }

  limpiar(): void {
    this.form.reset({ tipo: 'COMPETENCIA', torneoId: null, patrocinadorId: null, desde: '', hasta: '' });
    this.resultado.set(null);
    this.error.set('');
  }

  private filtrosActuales(): FiltrosReporte {
    const raw = this.form.getRawValue();
    return {
      tipo: raw.tipo,
      torneoId: raw.torneoId ?? undefined,
      patrocinadorId: raw.patrocinadorId ?? undefined,
      desde: raw.desde || undefined,
      hasta: raw.hasta || undefined
    };
  }

  private disparaDescarga(blob: Blob): void {
    const url = window.URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = `reporte-${this.form.controls.tipo.value.toLowerCase()}.pdf`;
    enlace.click();
    window.URL.revokeObjectURL(url);
  }
}
