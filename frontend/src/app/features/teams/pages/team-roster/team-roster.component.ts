import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Equipo } from '../../../../models/equipo.model';
import { MiembroEquipo, ROLES_EQUIPO } from '../../../../models/miembro-equipo.model';
import { TeamsService } from '../../services/teams.service';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-team-roster',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './team-roster.component.html',
  styleUrl: './team-roster.component.scss'
})
export class TeamRosterComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly teamsService = inject(TeamsService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly roles = ROLES_EQUIPO;
  readonly miembros = signal<MiembroEquipo[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly guardandoId = signal<number | null>(null);

  readonly invitando = signal(false);
  readonly errorInvitar = signal<string | null>(null);
  readonly invitacionEnviada = signal(false);
  readonly esCapitan = signal(false);

  // RF-03: disolución del equipo (solo el capitán; el backend lo valida).
  readonly confirmaDisolucion = signal(false);
  readonly motivoDisolucion = signal('');
  readonly disolviendo = signal(false);
  readonly errorDisolucion = signal<string | null>(null);
  readonly equipoDisuelto = signal<Equipo | null>(null);

  private equipoId!: number;

  readonly invitarForm = this.fb.nonNullable.group({
    jugadorId: [null as number | null, [Validators.required]],
    rolPropuesto: ['TITULAR', [Validators.required]],
    mensaje: ['']
  });

  ngOnInit(): void {
    this.equipoId = Number(this.route.snapshot.paramMap.get('equipoId'));
    this.cargarMiembros();
  }

  cargarMiembros(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.teamsService.listMiembros(this.equipoId).subscribe({
      next: (miembros) => {
        this.miembros.set(miembros);
        this.cargando.set(false);
        this.actualizarEsCapitan();
      },
      error: () => {
        this.error.set('No se pudo cargar la plantilla del equipo.');
        this.cargando.set(false);
      }
    });
  }

  private actualizarEsCapitan(): void {
    const miId = Number(this.authService.usuario()?.id);
    const soyCapitan = this.miembros().some(
      (m) => m.usuarioId === miId && m.rol === 'CAPITAN' && m.estado === 'ACTIVO'
    );
    this.esCapitan.set(soyCapitan);
  }

  cambiarRol(miembro: MiembroEquipo, nuevoRol: string): void {
    if (nuevoRol === miembro.rol) {
      return;
    }
    this.guardandoId.set(miembro.usuarioId);
    this.teamsService.cambiarRol(this.equipoId, miembro.usuarioId, { nuevoRol }).subscribe({
      next: () => {
        this.guardandoId.set(null);
        this.cargarMiembros();
      },
      error: (err) => {
        this.guardandoId.set(null);
        const mensaje = err?.error?.message ?? 'No se pudo cambiar el rol.';
        alert(mensaje);
        this.cargarMiembros();
      }
    });
  }

  invitar(): void {
    if (this.invitarForm.invalid) {
      this.invitarForm.markAllAsTouched();
      return;
    }

    this.invitando.set(true);
    this.errorInvitar.set(null);
    this.invitacionEnviada.set(false);
    const valores = this.invitarForm.getRawValue();

    this.teamsService.invitar(this.equipoId, {
      jugadorId: valores.jugadorId!,
      rolPropuesto: valores.rolPropuesto,
      mensaje: valores.mensaje || null
    }).subscribe({
      next: () => {
        this.invitando.set(false);
        this.invitacionEnviada.set(true);
        this.invitarForm.reset({ jugadorId: null, rolPropuesto: 'TITULAR', mensaje: '' });
      },
      error: (err) => {
        this.invitando.set(false);
        this.errorInvitar.set(err?.error?.message ?? 'No se pudo enviar la invitacion.');
      }
    });
  }

  disolverEquipo(): void {
    if (!this.confirmaDisolucion() || this.disolviendo() || this.equipoDisuelto()) {
      return;
    }
    this.disolviendo.set(true);
    this.errorDisolucion.set(null);
    this.teamsService.disolver(this.equipoId, {
      confirmacion: true,
      motivo: this.motivoDisolucion().trim() || null
    }).subscribe({
      next: (equipo) => {
        this.disolviendo.set(false);
        this.equipoDisuelto.set(equipo);
      },
      error: (err) => {
        this.disolviendo.set(false);
        this.errorDisolucion.set(err?.error?.message ?? 'No se pudo disolver el equipo.');
      }
    });
  }
}
