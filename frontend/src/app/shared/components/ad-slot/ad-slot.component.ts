import { Component, Input, OnInit, inject, signal } from '@angular/core';

import { EspaciosPublicitariosService } from '../../../features/sponsorships/services/espacios-publicitarios.service';
import { TransmisionesService } from '../../../features/transmisiones/services/transmisiones.service';
import { EspacioPublicitario } from '../../../models/espacio-publicitario.model';

@Component({
  selector: 'app-ad-slot',
  standalone: true,
  templateUrl: './ad-slot.component.html',
  styleUrl: './ad-slot.component.scss'
})
export class AdSlotComponent implements OnInit {
  @Input({ required: true }) ubicacion!: string;
  @Input() ligaId?: number;
  @Input() temporadaId?: number;
  @Input() torneoId?: number;

  private readonly espaciosService = inject(EspaciosPublicitariosService);
  private readonly transmisionesService = inject(TransmisionesService);

  readonly espacio = signal<EspacioPublicitario | null>(null);

  ngOnInit(): void {
    // DASHBOARD_CARD y CALENDARIO_FRANJA no viven dentro de una competencia
    // especifica: resuelven su alcance preguntando cual transmision esta
    // destacada ahora mismo. Si no hay ninguna destacada, no se muestra nada
    // (mismo comportamiento que cualquier otro ad-slot sin espacio vigente).
    if (this.ubicacion === 'DASHBOARD_CARD' || this.ubicacion === 'CALENDARIO_FRANJA') {
      this.resolverPorTransmisionDestacada();
      return;
    }

    this.buscarEspacio();
  }

  private resolverPorTransmisionDestacada(): void {
    this.transmisionesService.listar().subscribe({
      next: (respuesta) => {
        const destacada = respuesta.transmisiones.find((t) => t.destacada && t.torneoId != null);
        if (!destacada) {
          this.espacio.set(null);
          return;
        }
        this.torneoId = destacada.torneoId!;
        this.buscarEspacio();
      },
      error: () => this.espacio.set(null)
    });
  }

  private buscarEspacio(): void {
    this.espaciosService.buscarVigente({
      ubicacion: this.ubicacion,
      ligaId: this.ligaId,
      temporadaId: this.temporadaId,
      torneoId: this.torneoId
    }).subscribe((resultado) => this.espacio.set(resultado));
  }
}
