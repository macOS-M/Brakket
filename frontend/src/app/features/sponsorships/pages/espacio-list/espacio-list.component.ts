import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { EspacioPublicitario } from '../../../../models/espacio-publicitario.model';
import { EspaciosPublicitariosService } from '../../services/espacios-publicitarios.service';
import { AuthService } from '../../../../core/services/auth.service';
import { EtiquetaPipe } from '../../../../shared/pipes/etiqueta.pipe';

@Component({
  selector: 'app-espacio-list',
  standalone: true,
  imports: [RouterLink, DatePipe, EtiquetaPipe],
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
}
