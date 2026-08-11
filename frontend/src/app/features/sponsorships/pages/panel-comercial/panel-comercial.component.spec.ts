import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { PanelComercialComponent } from './panel-comercial.component';
import { PanelComercialService } from '../../services/panel-comercial.service';
import { MetricasPatrocinio, PanelComercial } from '../../../../models/panel-comercial.model';

describe('PanelComercialComponent', () => {
  let component: PanelComercialComponent;
  let fixture: ComponentFixture<PanelComercialComponent>;
  let panelServiceSpy: jasmine.SpyObj<PanelComercialService>;

  const panelMock: PanelComercial = {
    patrocinadorId: 11,
    patrocinadorNombre: 'AAAAAAA',
    patrocinios: [
      {
        patrocinioId: 1,
        nivel: 'ORO',
        estado: 'ACTIVO',
        vencido: false,
        ligaId: 8,
        temporadaId: null,
        torneoId: null,
        fechaInicio: '2014-01-05',
        fechaFin: '2026-09-03',
        cantidadEspacios: 2
      },
      {
        patrocinioId: 3,
        nivel: 'ORO',
        estado: 'ACTIVO',
        vencido: true,
        ligaId: null,
        temporadaId: null,
        torneoId: 11,
        fechaInicio: '2026-07-24',
        fechaFin: '2026-07-24',
        cantidadEspacios: 0
      }
    ]
  };

  const metricasMock: MetricasPatrocinio = {
    patrocinioId: 1,
    transmisionId: null,
    espectadoresPromedio: null,
    picoEspectadores: null,
    mensajesPorMinutoPromedio: null,
    sentimientoPredominante: null,
    sentimientoPendiente: true
  };

  beforeEach(async () => {
    panelServiceSpy = jasmine.createSpyObj('PanelComercialService', ['obtenerResumen', 'obtenerMetricas']);

    await TestBed.configureTestingModule({
      imports: [PanelComercialComponent],
      providers: [{ provide: PanelComercialService, useValue: panelServiceSpy }]
    }).compileComponents();
  });

  it('should create', () => {
    panelServiceSpy.obtenerResumen.and.returnValue(of(panelMock));
    panelServiceSpy.obtenerMetricas.and.returnValue(of(metricasMock));

    fixture = TestBed.createComponent(PanelComercialComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('carga el resumen y selecciona automaticamente el primer patrocinio', () => {
    panelServiceSpy.obtenerResumen.and.returnValue(of(panelMock));
    panelServiceSpy.obtenerMetricas.and.returnValue(of(metricasMock));

    fixture = TestBed.createComponent(PanelComercialComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.patrocinadorNombre()).toBe('AAAAAAA');
    expect(component.patrocinios().length).toBe(2);
    expect(component.patrocinioSeleccionadoId()).toBe(1);
    expect(panelServiceSpy.obtenerMetricas).toHaveBeenCalledWith(1);
  });

  it('muestra un mensaje de error si falla la carga del resumen', () => {
    panelServiceSpy.obtenerResumen.and.returnValue(throwError(() => ({ error: { message: 'Error de prueba' } })));

    fixture = TestBed.createComponent(PanelComercialComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.error()).toBe('Error de prueba');
    expect(component.cargando()).toBeFalse();
  });

  it('cambia el patrocinio seleccionado y recarga sus metricas al hacer clic', () => {
    panelServiceSpy.obtenerResumen.and.returnValue(of(panelMock));
    panelServiceSpy.obtenerMetricas.and.returnValue(of(metricasMock));

    fixture = TestBed.createComponent(PanelComercialComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.seleccionarPatrocinio(3);

    expect(component.patrocinioSeleccionadoId()).toBe(3);
    expect(panelServiceSpy.obtenerMetricas).toHaveBeenCalledWith(3);
  });

  it('calcula el texto de alcance correctamente segun el tipo', () => {
    panelServiceSpy.obtenerResumen.and.returnValue(of(panelMock));
    panelServiceSpy.obtenerMetricas.and.returnValue(of(metricasMock));

    fixture = TestBed.createComponent(PanelComercialComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.alcanceTexto(panelMock.patrocinios[0])).toBe('Liga #8');
    expect(component.alcanceTexto(panelMock.patrocinios[1])).toBe('Torneo #11');
  });
});
