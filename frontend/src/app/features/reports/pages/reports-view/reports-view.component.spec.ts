import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { ReportsViewComponent } from './reports-view.component';
import { ReportsService } from '../../services/reports.service';
import { SponsorshipsService } from '../../../sponsorships/services/sponsorships.service';
import { TournamentsService } from '../../../tournaments/services/tournaments.service';
import { ReporteResponse } from '../../../../models/reporte.model';

describe('ReportsViewComponent', () => {
  let component: ReportsViewComponent;
  let fixture: ComponentFixture<ReportsViewComponent>;
  let reportsSpy: jasmine.SpyObj<ReportsService>;
  let sponsorshipsSpy: jasmine.SpyObj<SponsorshipsService>;
  let tournamentsSpy: jasmine.SpyObj<TournamentsService>;

  const reporteDeEjemplo: ReporteResponse = {
    tipo: 'COMPETENCIA',
    titulo: 'Reporte de competencias y resultados',
    fechaGeneracion: '2026-08-12T20:00:00',
    usuarioSolicitante: 'Matías Calvo',
    filtrosDescripcion: 'Todos los torneos',
    columnas: ['Torneo', 'Ganador'],
    filas: [['Brakket Cup', 'Equipo A']]
  };

  beforeEach(async () => {
    reportsSpy = jasmine.createSpyObj('ReportsService', ['generar', 'generarPdf']);
    sponsorshipsSpy = jasmine.createSpyObj('SponsorshipsService', ['listar']);
    tournamentsSpy = jasmine.createSpyObj('TournamentsService', ['listar']);
    sponsorshipsSpy.listar.and.returnValue(of([]));
    tournamentsSpy.listar.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [ReportsViewComponent],
      providers: [
        { provide: ReportsService, useValue: reportsSpy },
        { provide: SponsorshipsService, useValue: sponsorshipsSpy },
        { provide: TournamentsService, useValue: tournamentsSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReportsViewComponent);
    component = fixture.componentInstance;
  });

  it('debe crearse y cargar torneos/patrocinadores al iniciar', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
    expect(tournamentsSpy.listar).toHaveBeenCalled();
    expect(sponsorshipsSpy.listar).toHaveBeenCalled();
  });

  it('generar() guarda el resultado cuando la API responde bien', () => {
    reportsSpy.generar.and.returnValue(of(reporteDeEjemplo));
    fixture.detectChanges();

    component.generar();

    expect(component.resultado()).toEqual(reporteDeEjemplo);
    expect(component.cargando()).toBeFalse();
    expect(component.error()).toBe('');
  });

  it('generar() muestra un error cuando la API falla', () => {
    reportsSpy.generar.and.returnValue(throwError(() => ({ error: { message: 'Sin permiso' } })));
    fixture.detectChanges();

    component.generar();

    expect(component.error()).toBe('Sin permiso');
    expect(component.resultado()).toBeNull();
  });

  it('generar() rechaza un rango de fechas invertido sin llamar a la API', () => {
    fixture.detectChanges();
    component.form.patchValue({ desde: '2026-08-20', hasta: '2026-08-01' });

    component.generar();

    expect(reportsSpy.generar).not.toHaveBeenCalled();
    expect(component.error()).toContain('fecha inicial');
  });

  it('limpiar() reinicia el formulario y el resultado', () => {
    reportsSpy.generar.and.returnValue(of(reporteDeEjemplo));
    fixture.detectChanges();
    component.generar();

    component.limpiar();

    expect(component.resultado()).toBeNull();
    expect(component.form.controls.torneoId.value).toBeNull();
  });
});
