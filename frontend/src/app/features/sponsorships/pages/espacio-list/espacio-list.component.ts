import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { EspacioPublicitario } from '../../../../models/espacio-publicitario.model';
import { EspaciosPublicitariosService } from '../../services/espacios-publicitarios.service';
import { AuthService } from '../../../../core/services/auth.service';
import { EtiquetaPipe } from '../../../../shared/pipes/etiqueta.pipe';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-espacio-list',
  standalone: true,
  imports: [RouterLink, DatePipe, EtiquetaPipe, PageHeaderComponent, EmptyStateComponent],
  templateUrl: './espacio-list.component.html',
  styleUrl: './espacio-list.component.scss'
})
export class EspacioListComponent implements OnInit {
  private readonly espaciosService = inject(EspaciosPublicitariosService);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  readonly espacios = signal<EspacioPublicitario[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly eliminandoId = signal<number | null>(null);
  patrocinioId!: number;

  readonly puedeGestionar = computed(() => {
    const roles = this.authService.usuario()?.roles ?? [];
    return roles.includes('ADMIN') || roles.includes('COMISIONADO');
  });

  ngOnInit(): void {
    this.patrocinioId = Number(this.route.snapshot.paramMap.get('patrocinioId'));
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.espaciosService.listarPorPatrocinio(this.patrocinioId).subscribe({
      next: (data) => {
        this.espacios.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el listado de espacios publicitarios.');
        this.cargando.set(false);
      }
    });
  }

  eliminar(e: EspacioPublicitario): void {
    const confirmado = window.confirm(
      `¿Eliminar el espacio "${e.ubicacion}"? Esta acción no se puede deshacer.`
    );
    if (!confirmado) return;

    this.eliminandoId.set(e.id);
    this.espaciosService.eliminar(e.id).subscribe({
      next: () => {
        this.espacios.update((lista) => lista.filter((x) => x.id !== e.id));
        this.eliminandoId.set(null);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo eliminar el espacio publicitario.');
        this.eliminandoId.set(null);
      }
    });
  }
}
