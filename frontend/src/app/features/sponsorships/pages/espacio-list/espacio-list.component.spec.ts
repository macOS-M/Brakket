import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';

import { EspacioListComponent } from './espacio-list.component';
import { EspaciosPublicitariosService } from '../../services/espacios-publicitarios.service';
import { AuthService } from '../../../../core/services/auth.service';
import { EspacioPublicitario } from '../../../../models/espacio-publicitario.model';

describe('EspacioListComponent', () => {
  let component: EspacioListComponent;
  let fixture: ComponentFixture<EspacioListComponent>;
  let espaciosServiceSpy: jasmine.SpyObj<EspaciosPublicitariosService>;

  const espacioMock: EspacioPublicitario = {
    id: 1,
    patrocinioId: 2,
    patrocinadorNombre: 'AAAAAAA',
    ubicacion: 'TORNEO_CABECERA',
    imagenUrl: 'https://ejemplo.com/banner.png',
    enlaceUrl: 'https://nike.com',
    estado: 'ACTIVO',
    fechaInicio: '2026-08-05',
    fechaFin: '2026-12-31'
  };

  beforeEach(async () => {
    espaciosServiceSpy = jasmine.createSpyObj('EspaciosPublicitariosService', ['listarPorPatrocinio']);

    const authServiceStub = {
      usuario: () => ({ roles: ['ADMIN'] })
    };

    const activatedRouteStub = {
      snapshot: {
        paramMap: {
          get: (key: string) => (key === 'patrocinioId' ? '2' : null)
        }
      }
    };

    await TestBed.configureTestingModule({
      imports: [EspacioListComponent],
      providers: [
        { provide: EspaciosPublicitariosService, useValue: espaciosServiceSpy },
        { provide: AuthService, useValue: authServiceStub },
        { provide: ActivatedRoute, useValue: activatedRouteStub }
      ]
    }).compileComponents();
  });

  it('should create', () => {
    espaciosServiceSpy.listarPorPatrocinio.and.returnValue(of([]));
    fixture = TestBed.createComponent(EspacioListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('lee el patrocinioId desde la ruta y carga sus espacios', () => {
    espaciosServiceSpy.listarPorPatrocinio.and.returnValue(of([espacioMock]));
    fixture = TestBed.createComponent(EspacioListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.patrocinioId).toBe(2);
    expect(espaciosServiceSpy.listarPorPatrocinio).toHaveBeenCalledWith(2);
    expect(component.espacios().length).toBe(1);
    expect(component.cargando()).toBeFalse();
  });

  it('muestra un mensaje de error si falla la carga', () => {
    espaciosServiceSpy.listarPorPatrocinio.and.returnValue(throwError(() => new Error('fallo')));
    fixture = TestBed.createComponent(EspacioListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.error()).toBe('No se pudo cargar el listado de espacios publicitarios.');
    expect(component.cargando()).toBeFalse();
  });

  it('puedeGestionar es true cuando el usuario tiene rol ADMIN', () => {
    espaciosServiceSpy.listarPorPatrocinio.and.returnValue(of([]));
    fixture = TestBed.createComponent(EspacioListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.puedeGestionar()).toBeTrue();
  });
});
