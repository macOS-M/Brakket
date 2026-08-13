import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AssociationListComponent } from './association-list.component';
import { PatrociniosService } from '../../services/patrocinios.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Patrocinio } from '../../../../models/patrocinio.model';

describe('AssociationListComponent', () => {
  let component: AssociationListComponent;
  let fixture: ComponentFixture<AssociationListComponent>;
  let patrociniosServiceSpy: jasmine.SpyObj<PatrociniosService>;

  const patrocinioMock: Patrocinio = {
    id: 1,
    patrocinadorId: 11,
    patrocinadorNombre: 'AAAAAAA',
    ligaId: null,
    temporadaId: null,
    torneoId: 12,
    condiciones: null,
    fechaInicio: '2026-08-05',
    fechaFin: '2026-12-31',
    estado: 'ACTIVO'
  };

  beforeEach(async () => {
    patrociniosServiceSpy = jasmine.createSpyObj('PatrociniosService', ['listarTodos']);

    const authServiceStub = {
      usuario: () => ({ roles: ['ADMIN'] })
    };

    await TestBed.configureTestingModule({
      imports: [AssociationListComponent],
      providers: [
        { provide: PatrociniosService, useValue: patrociniosServiceSpy },
        { provide: AuthService, useValue: authServiceStub },
        { provide: ActivatedRoute, useValue: {} }
      ]
    }).compileComponents();
  });

  it('should create', () => {
    patrociniosServiceSpy.listarTodos.and.returnValue(of([]));
    fixture = TestBed.createComponent(AssociationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('carga y muestra las asociaciones al iniciar', () => {
    patrociniosServiceSpy.listarTodos.and.returnValue(of([patrocinioMock]));
    fixture = TestBed.createComponent(AssociationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.patrocinios().length).toBe(1);
    expect(component.patrocinios()[0].patrocinadorNombre).toBe('AAAAAAA');
    expect(component.cargando()).toBeFalse();
  });

  it('muestra un mensaje de error si falla la carga', () => {
    patrociniosServiceSpy.listarTodos.and.returnValue(throwError(() => new Error('fallo')));
    fixture = TestBed.createComponent(AssociationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.error()).toBe('No se pudo cargar el listado de asociaciones.');
    expect(component.cargando()).toBeFalse();
  });

  it('calcula el texto de alcance correctamente segun el tipo', () => {
    fixture = TestBed.createComponent(AssociationListComponent);
    component = fixture.componentInstance;

    expect(component.alcanceTexto({ ...patrocinioMock, ligaId: 8, torneoId: null })).toBe('Liga #8');
    expect(component.alcanceTexto({ ...patrocinioMock, temporadaId: 5, torneoId: null })).toBe('Temporada #5');
    expect(component.alcanceTexto(patrocinioMock)).toBe('Torneo #12');
  });

  it('puedeGestionar es true cuando el usuario tiene rol ADMIN', () => {
    patrociniosServiceSpy.listarTodos.and.returnValue(of([]));
    fixture = TestBed.createComponent(AssociationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.puedeGestionar()).toBeTrue();
  });
});
