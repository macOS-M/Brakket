import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Patrocinio } from '../../../../models/patrocinio.model';
import { PatrociniosService } from '../../services/patrocinios.service';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-association-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './association-list.component.html',
  styleUrl: './association-list.component.scss'
})
export class AssociationListComponent implements OnInit {
  private readonly patrociniosService = inject(PatrociniosService);
  private readonly authService = inject(AuthService);

  readonly patrocinios = signal<Patrocinio[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

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
    this.patrociniosService.listarTodos().subscribe({
      next: (data) => {
        this.patrocinios.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el listado de asociaciones.');
        this.cargando.set(false);
      }
    });
  }

  alcanceTexto(p: Patrocinio): string {
    if (p.ligaId != null) return `Liga #${p.ligaId}`;
    if (p.temporadaId != null) return `Temporada #${p.temporadaId}`;
    return `Torneo #${p.torneoId}`;
  }
}
