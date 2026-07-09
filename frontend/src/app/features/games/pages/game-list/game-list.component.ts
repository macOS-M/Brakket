import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Juego } from '../../../../models/juego.model';
import { GamesService } from '../../services/games.service';

/**
 * Catalogo de juegos (RF-20).
 */
@Component({
  selector: 'app-game-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './game-list.component.html',
  styleUrl: './game-list.component.scss'
})
export class GameListComponent implements OnInit {
  private readonly gamesService = inject(GamesService);

  readonly juegos = signal<Juego[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.cargarJuegos();
  }

  cargarJuegos(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.gamesService.listActivos().subscribe({
      next: (juegos) => {
        this.juegos.set(juegos);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el catalogo de juegos.');
        this.cargando.set(false);
      }
    });
  }

  desactivar(juego: Juego): void {
    const confirmado = confirm(`¿Desactivar el juego "${juego.nombre}"?`);
    if (!confirmado) {
      return;
    }
    this.gamesService.desactivar(juego.id).subscribe({
      next: () => this.cargarJuegos(),
      error: (err) => {
        const mensaje = err?.error?.message ?? 'No se pudo desactivar el juego.';
        alert(mensaje);
      }
    });
  }
}
