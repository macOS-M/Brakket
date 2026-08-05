import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { Notificacion } from '../../../../models/notificacion.model';
import { NotificationsService } from '../../services/notifications.service';
import { NotificationListComponent } from './notification-list.component';

describe('NotificationListComponent', () => {
  let component: NotificationListComponent;
  let fixture: ComponentFixture<NotificationListComponent>;
  let service: jasmine.SpyObj<NotificationsService>;
  let router: jasmine.SpyObj<Router>;

  const disputa: Notificacion = {
    id: 1,
    tipo: 'DISPUTA',
    mensaje: 'Se abrió una disputa',
    origen: 'Arbitraje',
    entidad: 'disputa',
    entidadId: 50,
    leida: false,
    fecha: '2026-07-29T10:00:00',
    estadoEntrega: 'DISPONIBLE'
  };

  const transferencia: Notificacion = {
    ...disputa,
    id: 2,
    tipo: 'TRANSFERENCIA_SOLICITADA',
    mensaje: 'Se solicitó una transferencia',
    entidad: 'solicitud_transferencia',
    entidadId: 60,
    leida: true
  };

  beforeEach(async () => {
    service = jasmine.createSpyObj<NotificationsService>(
      'NotificationsService',
      ['list', 'markRead', 'markAllRead', 'remove', 'destination'],
      { cambios$: new Subject<void>().asObservable() }
    );
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    service.list.and.returnValue(of([disputa, transferencia]));
    service.markRead.and.callFake((id) =>
      of({ ...(id === disputa.id ? disputa : transferencia), leida: true })
    );
    service.markAllRead.and.returnValue(of(void 0));
    service.remove.and.returnValue(of(void 0));
    service.destination.and.callFake((item) =>
      item.tipo === 'DISPUTA' ? ['/disputes'] : ['/transfers']
    );
    router.navigate.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [NotificationListComponent],
      providers: [
        { provide: NotificationsService, useValue: service },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('carga las notificaciones y calcula las no leídas', () => {
    expect(service.list).toHaveBeenCalled();
    expect(component.notificaciones()).toEqual([disputa, transferencia]);
    expect(component.noLeidas()).toBe(1);
    expect(component.cargando()).toBeFalse();
  });

  it('filtra por categoría agrupando variantes de transferencia', () => {
    component.seleccionarFiltro('TRANSFERENCIA');

    expect(component.visibles()).toEqual([transferencia]);
  });

  it('marca una notificación como leída sin navegar desde la acción explícita', () => {
    const event = jasmine.createSpyObj<Event>('Event', ['stopPropagation']);

    component.marcarSoloComoLeida(disputa, event);

    expect(event.stopPropagation).toHaveBeenCalled();
    expect(service.markRead).toHaveBeenCalledWith(1);
    expect(component.notificaciones().find((item) => item.id === 1)?.leida).toBeTrue();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('marca todas las notificaciones como leídas', () => {
    component.marcarTodas();

    expect(service.markAllRead).toHaveBeenCalled();
    expect(component.noLeidas()).toBe(0);
  });

  it('retira una notificación de la bandeja', () => {
    const event = jasmine.createSpyObj<Event>('Event', ['stopPropagation']);

    component.eliminar(disputa, event);

    expect(service.remove).toHaveBeenCalledWith(1);
    expect(component.notificaciones()).toEqual([transferencia]);
  });

  it('marca como leída y navega al seleccionar una notificación', () => {
    component.marcarLeida(disputa);

    expect(service.markRead).toHaveBeenCalledWith(1);
    expect(service.destination).toHaveBeenCalledWith(disputa);
    expect(router.navigate).toHaveBeenCalledWith(['/disputes']);
  });

  it('navega sin volver a marcar una notificación que ya estaba leída', () => {
    component.marcarLeida(transferencia);

    expect(service.markRead).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/transfers']);
  });

  it('muestra un error cuando falla la carga', () => {
    service.list.and.returnValue(throwError(() => new Error('API caída')));

    component.cargar();

    expect(component.error()).toContain('No pudimos cargar');
    expect(component.cargando()).toBeFalse();
  });
});
