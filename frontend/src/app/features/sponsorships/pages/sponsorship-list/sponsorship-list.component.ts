import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Patrocinador } from '../../../../models/patrocinador.model';
import { SponsorshipsService } from '../../services/sponsorships.service';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-sponsorship-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './sponsorship-list.component.html',
  styleUrl: './sponsorship-list.component.scss'
})
export class SponsorshipListComponent implements OnInit {
  private readonly sponsorshipsService = inject(SponsorshipsService);
  private readonly authService = inject(AuthService);

  readonly patrocinadores = signal<Patrocinador[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly cambiandoId = signal<number | null>(null);

  readonly puedeGestionar = computed(() => {
    const roles = this.authService.usuario()?.roles ?? [];
    return roles.includes('ADMIN') || roles.includes('COMISIONADO');
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.sponsorshipsService.listar().subscribe({
      next: (patrocinadores) => {
        this.patrocinadores.set(patrocinadores);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el listado de patrocinadores.');
        this.cargando.set(false);
      }
    });
  }

  cambiarEstado(patrocinador: Patrocinador): void {
    const activar = patrocinador.estado !== 'ACTIVO';
    this.cambiandoId.set(patrocinador.id);
    this.sponsorshipsService.cambiarEstado(patrocinador.id, activar).subscribe({
      next: () => {
        this.cambiandoId.set(null);
        this.cargar();
      },
      error: (err) => {
        this.cambiandoId.set(null);
        alert(err?.error?.message ?? 'No se pudo cambiar el estado.');
      }
    });
  }
}
