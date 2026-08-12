import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

import { HistorialEquipoJugador } from '../../../../models/historial-jugador.model';
import { PlayersService } from '../../services/players.service';
import { RolEquipoPipe } from '../../../../shared/pipes/rol-equipo.pipe';

@Component({
  selector: 'app-player-history',
  standalone: true,
  imports: [DatePipe, RolEquipoPipe],
  templateUrl: './player-history.component.html',
  styleUrl: './player-history.component.scss'
})
export class PlayerHistoryComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly playersService = inject(PlayersService);

  readonly historial = signal<HistorialEquipoJugador[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  private jugadorId!: number;

  ngOnInit(): void {
    this.jugadorId = Number(this.route.snapshot.paramMap.get('jugadorId'));
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.playersService.historial(this.jugadorId, null, null, null).subscribe({
      next: (historial) => {
        this.historial.set(historial);
        this.cargando.set(false);
      },
      error: (err) => {
        this.cargando.set(false);
        if (err?.status === 403) {
          this.error.set('Este jugador mantiene su historial en privado.');
        } else if (err?.status === 404) {
          this.error.set('No se encontró este jugador.');
        } else {
          this.error.set('No se pudo cargar el historial.');
        }
      }
    });
  }
}
