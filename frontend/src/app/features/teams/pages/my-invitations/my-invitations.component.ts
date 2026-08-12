import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';

import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { Invitacion } from '../../../../models/invitacion.model';
import { TeamsService } from '../../services/teams.service';
import { RolEquipoPipe } from '../../../../shared/pipes/rol-equipo.pipe';

@Component({
  selector: 'app-my-invitations',
  standalone: true,
  imports: [DatePipe, PageHeaderComponent, EmptyStateComponent, StatusBadgeComponent, RolEquipoPipe],
  templateUrl: './my-invitations.component.html',
  styleUrl: './my-invitations.component.scss'
})
export class MyInvitationsComponent implements OnInit {
  private readonly teamsService = inject(TeamsService);

  readonly invitaciones = signal<Invitacion[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly respondiendoId = signal<number | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.teamsService.misInvitacionesPendientes().subscribe({
      next: (invitaciones) => {
        this.invitaciones.set(invitaciones);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar tus invitaciones.');
        this.cargando.set(false);
      }
    });
  }

  responder(invitacion: Invitacion, aceptar: boolean): void {
    this.respondiendoId.set(invitacion.id);
    this.teamsService.responderInvitacion(invitacion.id, { aceptar }).subscribe({
      next: () => {
        this.respondiendoId.set(null);
        this.cargar();
      },
      error: (err) => {
        this.respondiendoId.set(null);
        alert(err?.error?.message ?? 'No se pudo procesar la respuesta.');
        this.cargar();
      }
    });
  }
}
