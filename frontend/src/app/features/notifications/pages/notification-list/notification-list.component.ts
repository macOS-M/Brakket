import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { Notificacion, TipoNotificacion } from '../../../../models/notificacion.model';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { fechaHoraMedia } from '../../../../shared/utils/formato-fecha';
import { NotificationsService } from '../../services/notifications.service';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [PageHeaderComponent],
  templateUrl: './notification-list.component.html',
  styleUrl: './notification-list.component.scss'
})
export class NotificationListComponent {
  private readonly service = inject(NotificationsService);
  private readonly router = inject(Router);

  readonly notificaciones = signal<Notificacion[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly filtro = signal<'TODAS' | 'NO_LEIDAS' | TipoNotificacion>('TODAS');
  readonly procesandoTodas = signal(false);

  readonly visibles = computed(() => {
    const filtro = this.filtro();
    if (filtro === 'TODAS') return this.notificaciones();
    if (filtro === 'NO_LEIDAS') return this.notificaciones().filter((n) => !n.leida);
    return this.notificaciones().filter((n) => this.categoria(n.tipo) === filtro);
  });

  readonly noLeidas = computed(() => this.notificaciones().filter((n) => !n.leida).length);

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.service.list().pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: (items) => this.notificaciones.set(items),
      error: () => this.error.set('No pudimos cargar tus notificaciones. Inténtalo nuevamente.')
    });
  }

  seleccionarFiltro(filtro: 'TODAS' | 'NO_LEIDAS' | TipoNotificacion): void {
    this.filtro.set(filtro);
  }

  marcarLeida(notificacion: Notificacion): void {
    if (!notificacion.leida) {
      this.service.markRead(notificacion.id).subscribe({
        next: (actualizada) => this.reemplazar(actualizada),
        error: () => this.error.set('No se pudo actualizar la notificación.')
      });
    }
    this.router.navigate(this.service.destination(notificacion));
  }

  marcarSoloComoLeida(notificacion: Notificacion, evento: Event): void {
    evento.stopPropagation();
    if (notificacion.leida) return;
    this.service.markRead(notificacion.id).subscribe({
      next: (actualizada) => this.reemplazar(actualizada),
      error: () => this.error.set('No se pudo actualizar la notificación.')
    });
  }

  marcarTodas(): void {
    if (!this.noLeidas() || this.procesandoTodas()) return;
    this.procesandoTodas.set(true);
    this.service.markAllRead().pipe(finalize(() => this.procesandoTodas.set(false))).subscribe({
      next: () => this.notificaciones.update((items) => items.map((n) => ({ ...n, leida: true }))),
      error: () => this.error.set('No se pudieron marcar todas como leídas.')
    });
  }

  eliminar(notificacion: Notificacion, evento: Event): void {
    evento.stopPropagation();
    this.service.remove(notificacion.id).subscribe({
      next: () => this.notificaciones.update((items) => items.filter((n) => n.id !== notificacion.id)),
      error: () => this.error.set('No se pudo quitar la notificación de la bandeja.')
    });
  }

  etiqueta(tipo: TipoNotificacion): string {
    return ({
      INVITACION: 'Invitación', TRANSFERENCIA: 'Transferencia', RESULTADO: 'Resultado',
      DISPUTA: 'Disputa', CAMBIO_TORNEO: 'Torneo', TRANSMISION: 'Transmisión',
      ADMINISTRATIVA: 'Administrativa', EXPULSION_EQUIPO: 'Equipo', CORRECCION: 'Corrección'
    })[this.categoria(tipo)];
  }

  icono(tipo: TipoNotificacion): string {
    return ({
      INVITACION: '✦', TRANSFERENCIA: '⇄', RESULTADO: '✓', DISPUTA: '!',
      CAMBIO_TORNEO: '◆', TRANSMISION: '●', ADMINISTRATIVA: 'i',
      EXPULSION_EQUIPO: '−', CORRECCION: '↻'
    })[this.categoria(tipo)];
  }

  fecha(fecha: string): string {
    return fechaHoraMedia(fecha);
  }

  private reemplazar(actualizada: Notificacion): void {
    this.notificaciones.update((items) =>
      items.map((n) => n.id === actualizada.id ? actualizada : n)
    );
  }

  categoria(tipo: TipoNotificacion):
    'INVITACION' | 'TRANSFERENCIA' | 'RESULTADO' | 'DISPUTA' | 'CAMBIO_TORNEO' |
    'TRANSMISION' | 'ADMINISTRATIVA' | 'EXPULSION_EQUIPO' | 'CORRECCION' {
    if (tipo.startsWith('INVITACION') || tipo.startsWith('SOLICITUD_')) return 'INVITACION';
    if (tipo.startsWith('TRANSFERENCIA_')) return 'TRANSFERENCIA';
    return tipo as 'RESULTADO' | 'DISPUTA' | 'CAMBIO_TORNEO' | 'TRANSMISION' |
      'ADMINISTRATIVA' | 'EXPULSION_EQUIPO' | 'CORRECCION' | 'INVITACION' | 'TRANSFERENCIA';
  }
}
