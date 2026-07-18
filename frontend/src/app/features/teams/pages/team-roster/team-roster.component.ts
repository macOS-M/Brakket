import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Equipo } from '../../../../models/equipo.model';
import { MiembroEquipo, ROLES_EQUIPO } from '../../../../models/miembro-equipo.model';
import { TeamsService } from '../../services/teams.service';
import { AuthService } from '../../../../core/services/auth.service';

/** Orden de presentación de la plantilla (RF-08): capitán primero. */
const ORDEN_ROLES: Record<string, number> = { CAPITAN: 0, TITULAR: 1, SUPLENTE: 2, COACH: 3 };

@Component({
  selector: 'app-team-roster',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe],
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

  // RF-08: datos del equipo para encabezado y vista histórica si está disuelto.
  readonly equipo = signal<Equipo | null>(null);

  /** Plantilla ordenada: activos primero (capitán, titulares, suplentes, coach), luego el resto. */
  readonly miembrosOrdenados = computed(() =>
    [...this.miembros()].sort((a, b) => {
      const activoA = a.estado === 'ACTIVO' ? 0 : 1;
      const activoB = b.estado === 'ACTIVO' ? 0 : 1;
      if (activoA !== activoB) return activoA - activoB;
      const rolA = ORDEN_ROLES[a.rol] ?? 9;
      const rolB = ORDEN_ROLES[b.rol] ?? 9;
      if (rolA !== rolB) return rolA - rolB;
      return a.nombreUsuario.localeCompare(b.nombreUsuario);
    })
  );

  /** El equipo está disuelto: la plantilla se muestra como vista histórica de solo lectura. */
  readonly vistaHistorica = computed(
    () => this.equipoDisuelto() !== null || this.equipo()?.estado === 'DISUELTO'
  );

  readonly invitando = signal(false);
  readonly errorInvitar = signal<string | null>(null);
  readonly invitacionEnviada = signal(false);
  readonly esCapitan = signal(false);

  // RF-10: expulsión de integrantes (solo el capitán; el backend lo valida).
  readonly miembroAExpulsar = signal<MiembroEquipo | null>(null);
  readonly causaExpulsion = signal('');
  readonly expulsando = signal(false);
  readonly errorExpulsion = signal<string | null>(null);

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
    this.cargarEquipo();
    this.cargarMiembros();
  }

  /** RF-08: carga nombre y estado del equipo; si falla, la plantilla sigue siendo usable. */
  private cargarEquipo(): void {
    this.teamsService.obtenerPorId(this.equipoId).subscribe({
      next: (equipo) => this.equipo.set(equipo),
      error: () => this.equipo.set(null)
    });
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
      error: (err) => {
        this.error.set(err?.status === 404
          ? 'El equipo consultado no existe.'
          : 'No se pudo cargar la plantilla del equipo.');
        this.cargando.set(false);
      }
    });
  }

  /** Reintento del criterio "error de carga" de RF-08. */
  reintentar(): void {
    if (!this.equipo()) {
      this.cargarEquipo();
    }
    this.cargarMiembros();
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

  /** RF-10: abre la confirmación de expulsión para un integrante activo. */
  iniciarExpulsion(miembro: MiembroEquipo): void {
    this.miembroAExpulsar.set(miembro);
    this.causaExpulsion.set('');
    this.errorExpulsion.set(null);
  }

  /** RF-10: cancelar la confirmación no realiza la baja. */
  cancelarExpulsion(): void {
    this.miembroAExpulsar.set(null);
    this.causaExpulsion.set('');
    this.errorExpulsion.set(null);
  }

  confirmarExpulsion(): void {
    const miembro = this.miembroAExpulsar();
    if (!miembro || this.expulsando()) {
      return;
    }
    const causa = this.causaExpulsion().trim();
    if (!causa) {
      this.errorExpulsion.set('La causa de la expulsión es obligatoria.');
      return;
    }
    this.expulsando.set(true);
    this.errorExpulsion.set(null);
    this.teamsService.expulsar(this.equipoId, miembro.usuarioId, { causa }).subscribe({
      next: () => {
        this.expulsando.set(false);
        this.cancelarExpulsion();
        this.cargarMiembros();
      },
      error: (err) => {
        this.expulsando.set(false);
        this.errorExpulsion.set(err?.error?.message ?? 'No se pudo expulsar al integrante.');
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
