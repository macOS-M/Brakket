import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { PanelComercialService } from '../../services/panel-comercial.service';
import { MetricasPatrocinio, PatrocinioResumen } from '../../../../models/panel-comercial.model';

@Component({
  selector: 'app-panel-comercial',
  standalone: true,
  imports: [],
  templateUrl: './panel-comercial.component.html',
  styleUrl: './panel-comercial.component.scss'
})
export class PanelComercialComponent implements OnInit {
  private readonly panelService = inject(PanelComercialService);

  readonly patrocinadorNombre = signal<string>('');
  readonly patrocinios = signal<PatrocinioResumen[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly patrocinioSeleccionadoId = signal<number | null>(null);
  readonly metricas = signal<MetricasPatrocinio | null>(null);
  readonly cargandoMetricas = signal(false);

  readonly patrocinioSeleccionado = computed(() =>
    this.patrocinios().find((p) => p.patrocinioId === this.patrocinioSeleccionadoId()) ?? null
  );

  ngOnInit(): void {
    this.panelService.obtenerResumen().subscribe({
      next: (panel) => {
        this.patrocinadorNombre.set(panel.patrocinadorNombre);
        this.patrocinios.set(panel.patrocinios);
        this.cargando.set(false);

        if (panel.patrocinios.length > 0) {
          this.seleccionarPatrocinio(panel.patrocinios[0].patrocinioId);
        }
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo cargar tu panel comercial.');
        this.cargando.set(false);
      }
    });
  }

  seleccionarPatrocinio(patrocinioId: number): void {
    this.patrocinioSeleccionadoId.set(patrocinioId);
    this.metricas.set(null);
    this.cargandoMetricas.set(true);

    this.panelService.obtenerMetricas(patrocinioId).subscribe({
      next: (data) => {
        this.metricas.set(data);
        this.cargandoMetricas.set(false);
      },
      error: () => {
        this.cargandoMetricas.set(false);
      }
    });
  }

  alcanceTexto(p: PatrocinioResumen): string {
    if (p.ligaId != null) return `Liga #${p.ligaId}`;
    if (p.temporadaId != null) return `Temporada #${p.temporadaId}`;
    return `Torneo #${p.torneoId}`;
  }
}
