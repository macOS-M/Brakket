import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { Patrocinio } from '../../../../models/patrocinio.model';
import { PatrociniosService } from '../../services/patrocinios.service';
import { AuthService } from '../../../../core/services/auth.service';
import { EtiquetaPipe } from '../../../../shared/pipes/etiqueta.pipe';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-association-list',
  standalone: true,
  imports: [RouterLink, DatePipe, EtiquetaPipe, PageHeaderComponent, EmptyStateComponent],
  templateUrl: './association-list.component.html',
  styleUrl: './association-list.component.scss'
})
export class AssociationListComponent implements OnInit {
  private readonly patrociniosService = inject(PatrociniosService);
  private readonly authService = inject(AuthService);

  readonly patrocinios = signal<Patrocinio[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly eliminandoId = signal<number | null>(null);

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

  /** Color determinístico a partir del nombre, para el avatar cuando no hay
   * logo del patrocinador disponible en este DTO. Mismo hash simple para
   * que un mismo patrocinador siempre salga con el mismo color en toda la
   * grilla, sin depender de datos nuevos del backend. */
  colorDe(nombre: string): string {
    let hash = 0;
    for (let i = 0; i < nombre.length; i++) {
      hash = nombre.charCodeAt(i) + ((hash << 5) - hash);
    }
    const tono = Math.abs(hash) % 360;
    return `hsl(${tono}, 45%, 32%)`;
  }

  eliminar(p: Patrocinio): void {
    const confirmado = window.confirm(
      `¿Eliminar la asociación con ${p.patrocinadorNombre}? Esto también borra sus espacios publicitarios. Esta acción no se puede deshacer.`
    );
    if (!confirmado) return;

    this.eliminandoId.set(p.id);
    this.patrociniosService.eliminar(p.id).subscribe({
      next: () => {
        this.patrocinios.update((lista) => lista.filter((x) => x.id !== p.id));
        this.eliminandoId.set(null);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo eliminar la asociación.');
        this.eliminandoId.set(null);
      }
    });
  }
}
