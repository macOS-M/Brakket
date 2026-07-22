import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { EquipoResumenPublico } from '../../../../models/perfil-equipo-publico.model';
import { MiembroEquipo } from '../../../../models/miembro-equipo.model';
import { ROLES_PROPUESTOS } from '../../../../models/transferencia.model';
import { TeamsService } from '../../../teams/services/teams.service';
import { TransfersService } from '../../services/transfers.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';

/**
 * Solicitar la transferencia de un jugador de otro equipo (RF-12).
 * El solicitante debe ser el capitán del equipo destino; el backend
 * valida cupo, pertenencia y duplicados.
 */
@Component({
  selector: 'app-transfer-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, PageHeaderComponent],
  templateUrl: './transfer-form.component.html',
  styleUrl: './transfer-form.component.scss'
})
export class TransferFormComponent implements OnInit {
  private readonly transfersService = inject(TransfersService);
  private readonly teamsService = inject(TeamsService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly rolesPropuestos = ROLES_PROPUESTOS;

  readonly form = this.fb.nonNullable.group({
    equipoOrigenId: ['', Validators.required],
    jugadorId: ['', Validators.required],
    equipoDestinoId: ['', Validators.required],
    rolPropuesto: ['TITULAR', Validators.required],
    justificacion: ['', Validators.maxLength(500)]
  });

  readonly equipos = signal<EquipoResumenPublico[]>([]);
  readonly miembrosOrigen = signal<MiembroEquipo[]>([]);
  readonly cargandoMiembros = signal(false);
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.teamsService.listarPublicos().subscribe({
      next: (equipos) => this.equipos.set(equipos),
      error: () => this.error.set('No se pudo cargar la lista de equipos.')
    });

    this.form.controls.equipoOrigenId.valueChanges.subscribe((equipoId) => {
      this.form.controls.jugadorId.setValue('');
      this.miembrosOrigen.set([]);
      if (equipoId) {
        this.cargarMiembros(Number(equipoId));
      }
    });
  }

  guardar(): void {
    if (this.form.invalid || this.enviando()) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    this.enviando.set(true);
    this.error.set(null);
    this.transfersService
      .solicitar({
        jugadorId: Number(valor.jugadorId),
        equipoOrigenId: Number(valor.equipoOrigenId),
        equipoDestinoId: Number(valor.equipoDestinoId),
        rolPropuesto: valor.rolPropuesto,
        justificacion: valor.justificacion.trim() || null
      })
      .subscribe({
        next: () => this.router.navigate(['/transfers']),
        error: (err) => {
          this.error.set(err?.error?.message ?? 'No se pudo crear la solicitud de transferencia.');
          this.enviando.set(false);
        }
      });
  }

  private cargarMiembros(equipoId: number): void {
    this.cargandoMiembros.set(true);
    this.teamsService.listMiembros(equipoId).subscribe({
      next: (miembros) => {
        // Solo miembros activos y no capitanes: el capitán no es transferible.
        this.miembrosOrigen.set(
          miembros.filter((m) => m.estado === 'ACTIVO' && m.rol !== 'CAPITAN')
        );
        this.cargandoMiembros.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los integrantes del equipo de origen.');
        this.cargandoMiembros.set(false);
      }
    });
  }
}
