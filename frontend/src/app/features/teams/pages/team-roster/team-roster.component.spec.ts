import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';

import { TeamRosterComponent } from './team-roster.component';
import { TeamsService } from '../../services/teams.service';
import { AuthService } from '../../../../core/services/auth.service';
import { GamesService } from '../../../games/services/games.service';
import { MiembroEquipo } from '../../../../models/miembro-equipo.model';
import { Equipo } from '../../../../models/equipo.model';

describe('TeamRosterComponent', () => {
  let component: TeamRosterComponent;
  let fixture: ComponentFixture<TeamRosterComponent>;
  let teamsService: TeamsService;

  const equipoActivo = {
    id: 1,
    nombre: 'Coffee&Commits',
    logo: null,
    descripcion: null,
    estado: 'ACTIVO',
    fechaDisolucion: null,
    motivoDisolucion: null
  };

  const equipoDisuelto = {
    ...equipoActivo,
    estado: 'DISUELTO',
    fechaDisolucion: '2026-07-10T12:00:00',
    motivoDisolucion: 'Fin de temporada'
  };

  const miembro = (over: Partial<MiembroEquipo>): MiembroEquipo => ({
    id: 1,
    equipoId: 1,
    usuarioId: 1,
    nombreUsuario: 'Jugador',
    rol: 'TITULAR',
    estado: 'ACTIVO',
    fechaUnion: '2026-07-01',
    ...over
  });

  beforeEach(async () => {
    const teamsServiceMock = {
      listMiembros: () => of([]),
      obtenerPorId: () => of(equipoActivo),
      cambiarRol: () => of({}),
      disolver: () => of(equipoDisuelto),
      invitar: () => of({}),
      expulsar: () => of({}),
      solicitudesPendientes: () => of([]),
      responderSolicitud: () => of({})
    };

    const authServiceMock = {
      usuario: () => ({ id: 1 })
    };

    // El componente inyecta GamesService (filtro de juegos de RF-11) y lo llama
    // en ngOnInit; sin este mock el test pide un HttpClient que no está provisto.
    const gamesServiceMock = {
      listActivos: () => of([])
    };

    await TestBed.configureTestingModule({
      imports: [TeamRosterComponent],
      providers: [
        { provide: TeamsService, useValue: teamsServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: GamesService, useValue: gamesServiceMock },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '1' } } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TeamRosterComponent);
    component = fixture.componentInstance;
    teamsService = TestBed.inject(TeamsService);
  });

  const init = () => fixture.detectChanges();

  it('should create', () => {
    init();
    expect(component).toBeTruthy();
  });

  it('should load an empty roster without error', () => {
    init();
    expect(component.miembros().length).toBe(0);
    expect(component.error()).toBeNull();
  });

  // RF-08: la plantilla se ordena con el capitán primero y los inactivos al final.
  it('should order the roster: captain first, inactive members last', () => {
    spyOn(teamsService, 'listMiembros').and.returnValue(of([
      miembro({ id: 3, usuarioId: 3, nombreUsuario: 'Suplente', rol: 'SUPLENTE' }),
      miembro({ id: 4, usuarioId: 4, nombreUsuario: 'Inactivo', rol: 'TITULAR', estado: 'INACTIVO' }),
      miembro({ id: 2, usuarioId: 2, nombreUsuario: 'Capi', rol: 'CAPITAN' }),
      miembro({ id: 5, usuarioId: 5, nombreUsuario: 'Titular', rol: 'TITULAR' })
    ]));
    init();

    expect(component.miembrosOrdenados().map((m) => m.nombreUsuario))
      .toEqual(['Capi', 'Titular', 'Suplente', 'Inactivo']);
  });

  // RF-08: si el equipo está disuelto, la plantilla es una vista histórica.
  it('should mark the view as historical when the team is dissolved', () => {
    spyOn(teamsService, 'obtenerPorId').and.returnValue(of(equipoDisuelto as unknown as Equipo));
    init();

    expect(component.vistaHistorica()).toBeTrue();
  });

  it('should not mark the view as historical for an active team', () => {
    init();
    expect(component.vistaHistorica()).toBeFalse();
  });

  // RF-08: el equipo consultado debe existir.
  it('should show a specific message when the team does not exist', () => {
    spyOn(teamsService, 'listMiembros').and.returnValue(throwError(() => ({ status: 404 })));
    init();

    expect(component.error()).toBe('El equipo consultado no existe.');
  });

  // RF-08: ante un error de carga se puede reintentar.
  it('should reload the roster when retrying after an error', () => {
    const spy = spyOn(teamsService, 'listMiembros').and.returnValue(throwError(() => ({ status: 500 })));
    init();
    expect(component.error()).toBe('No se pudo cargar la plantilla del equipo.');

    spy.and.returnValue(of([miembro({})]));
    component.reintentar();

    expect(component.error()).toBeNull();
    expect(component.miembros().length).toBe(1);
  });

  it('should not dissolve the team without explicit confirmation', () => {
    init();
    const spy = spyOn(teamsService, 'disolver').and.callThrough();

    component.disolverEquipo();

    expect(spy).not.toHaveBeenCalled();
    expect(component.equipoDisuelto()).toBeNull();
  });

  it('should dissolve the team when confirmed, sending the trimmed optional reason', () => {
    init();
    const spy = spyOn(teamsService, 'disolver').and.callThrough();
    component.confirmaDisolucion.set(true);
    component.motivoDisolucion.set('  Fin de temporada  ');

    component.disolverEquipo();

    expect(spy).toHaveBeenCalledWith(1, { confirmacion: true, motivo: 'Fin de temporada' });
    expect(component.equipoDisuelto()?.estado).toBe('DISUELTO');
  });

  it('should send a null reason when the reason is left empty', () => {
    init();
    const spy = spyOn(teamsService, 'disolver').and.callThrough();
    component.confirmaDisolucion.set(true);

    component.disolverEquipo();

    expect(spy).toHaveBeenCalledWith(1, { confirmacion: true, motivo: null });
  });

  it('should not send invitations when nobody is selected', () => {
    init();
    const spy = spyOn(teamsService, 'invitar').and.callThrough();

    component.invitarSeleccionados();

    expect(spy).not.toHaveBeenCalled();
  });

  // Selección múltiple: una invitación por jugador, cada uno con su rol.
  it('should send one invitation per selected player with its own role', () => {
    init();
    const spy = spyOn(teamsService, 'invitar').and.callThrough();
    component.seleccionados.set([
      { id: 7, nombre: 'Ana', rol: 'TITULAR' },
      { id: 8, nombre: 'Beto', rol: 'SUPLENTE' }
    ]);

    component.invitarSeleccionados();

    expect(spy).toHaveBeenCalledWith(1, { jugadorId: 7, rolPropuesto: 'TITULAR', mensaje: null });
    expect(spy).toHaveBeenCalledWith(1, { jugadorId: 8, rolPropuesto: 'SUPLENTE', mensaje: null });
    expect(component.seleccionados().length).toBe(0);
    expect(component.resumenInvitaciones()).toBe('2 invitaciones enviadas.');
  });

  // RF-10: la causa de la expulsión es obligatoria.
  it('should not expel a member without a cause', () => {
    init();
    const spy = spyOn(teamsService, 'expulsar').and.callThrough();
    component.iniciarExpulsion(miembro({ usuarioId: 2 }));
    component.causaExpulsion.set('   ');

    component.confirmarExpulsion();

    expect(spy).not.toHaveBeenCalled();
    expect(component.errorExpulsion()).toBe('La causa de la expulsión es obligatoria.');
  });

  // RF-10: al confirmar se envía la causa recortada y se recarga la plantilla.
  it('should expel the member with the trimmed cause and reload the roster', () => {
    init();
    const expulsarSpy = spyOn(teamsService, 'expulsar').and.callThrough();
    const reloadSpy = spyOn(teamsService, 'listMiembros').and.callThrough();
    component.iniciarExpulsion(miembro({ usuarioId: 2 }));
    component.causaExpulsion.set('  Inasistencia reiterada  ');

    component.confirmarExpulsion();

    expect(expulsarSpy).toHaveBeenCalledWith(1, 2, { causa: 'Inasistencia reiterada' });
    expect(component.miembroAExpulsar()).toBeNull();
    expect(reloadSpy).toHaveBeenCalled();
  });

  // RF-10: cancelar la confirmación no realiza la baja.
  it('should not expel when the confirmation is cancelled', () => {
    init();
    const spy = spyOn(teamsService, 'expulsar').and.callThrough();
    component.iniciarExpulsion(miembro({ usuarioId: 2 }));
    component.causaExpulsion.set('Inasistencia');

    component.cancelarExpulsion();
    component.confirmarExpulsion();

    expect(spy).not.toHaveBeenCalled();
    expect(component.miembroAExpulsar()).toBeNull();
  });

  it('should surface the backend message when the expulsion fails', () => {
    init();
    spyOn(teamsService, 'expulsar').and.returnValue(
      throwError(() => ({ error: { message: 'No se puede expulsar al unico capitan' } }))
    );
    component.iniciarExpulsion(miembro({ usuarioId: 2 }));
    component.causaExpulsion.set('Inasistencia');

    component.confirmarExpulsion();

    expect(component.errorExpulsion()).toBe('No se puede expulsar al unico capitan');
    expect(component.miembroAExpulsar()).not.toBeNull();
  });
});
