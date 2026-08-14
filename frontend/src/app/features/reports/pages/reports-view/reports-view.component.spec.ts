import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { ReportsViewComponent } from './reports-view.component';
import { ReportsService } from '../../services/reports.service';
import { SponsorshipsService } from '../../../sponsorships/services/sponsorships.service';
import { TournamentsService } from '../../../tournaments/services/tournaments.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ReporteResponse } from '../../../../models/reporte.model';

describe('ReportsViewComponent', () => {
  let component: ReportsViewComponent;
  let fixture: ComponentFixture<ReportsViewComponent>;
  let reportsSpy: jasmine.SpyObj<ReportsService>;
  let sponsorshipsSpy: jasmine.SpyObj<SponsorshipsService>;
  let tournamentsSpy: jasmine.SpyObj<TournamentsService>;
  let authServiceStub: { usuario: () => { roles: string[] } };

  const reporteDeEjemplo: ReporteResponse = {
    tipo: 'COMPETENCIA',
    titulo: 'Reporte de competencias y resultados',
    fechaGeneracion: '2026-08-12T20:00:00',
    usuarioSolicitante: 'Matías Calvo',
    filtrosDescripcion: 'Todos los torneos',
    columnas: ['Torneo', 'Ganador'],
    filas: [['Brakket Cup', 'Equipo A']]
  };

  function configurarModulo(roles: string[]): void {
    reportsSpy = jasmine.createSpyObj('ReportsService', ['generar', 'generarPdf']);
    sponsorshipsSpy = jasmine.createSpyObj('SponsorshipsService', ['listar']);
    tournamentsSpy = jasmine.createSpyObj('TournamentsService', ['listar']);
    sponsorshipsSpy.listar.and.returnValue(of([]));
    tournamentsSpy.listar.and.returnValue(of([]));
    authServiceStub = { usuario: () => ({ roles }) };

    TestBed.configureTestingModule({
      imports: [ReportsViewComponent],
      providers: [
        { provide: ReportsService, useValue: reportsSpy },
        { provide: SponsorshipsService, useValue: sponsorshipsSpy },
        { provide: TournamentsService, useValue: tournamentsSpy },
        { provide: AuthService, useValue: authServiceStub }
      ]
    });
  }

  beforeEach(async () => {
    configurarModulo(['ADMIN']);
    await TestBed.compileComponents();

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

  // ---------------------------------------------------------------
  // esPatrocinadorSolo: oculta el selector de patrocinador cuando quien
  // mira el reporte es exclusivamente PATROCINADOR (el backend ya lo
  // ignoraba; esto es solo dejar de mostrar una opción sin efecto real).
  // ---------------------------------------------------------------

  it('esPatrocinadorSolo es false para ADMIN', () => {
    fixture.detectChanges();
    expect(component.esPatrocinadorSolo()).toBeFalse();
  });
});

describe('ReportsViewComponent — rol PATROCINADOR', () => {
  let component: ReportsViewComponent;
  let fixture: ComponentFixture<ReportsViewComponent>;
  let sponsorshipsSpy: jasmine.SpyObj<SponsorshipsService>;
  let tournamentsSpy: jasmine.SpyObj<TournamentsService>;

  beforeEach(async () => {
    const reportsSpy = jasmine.createSpyObj('ReportsService', ['generar', 'generarPdf']);
    sponsorshipsSpy = jasmine.createSpyObj('SponsorshipsService', ['listar']);
    tournamentsSpy = jasmine.createSpyObj('TournamentsService', ['listar']);
    sponsorshipsSpy.listar.and.returnValue(of([]));
    tournamentsSpy.listar.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [ReportsViewComponent],
      providers: [
        { provide: ReportsService, useValue: reportsSpy },
        { provide: SponsorshipsService, useValue: sponsorshipsSpy },
        { provide: TournamentsService, useValue: tournamentsSpy },
        { provide: AuthService, useValue: { usuario: () => ({ roles: ['PATROCINADOR'] }) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReportsViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('esPatrocinadorSolo es true cuando el único rol es PATROCINADOR', () => {
    expect(component.esPatrocinadorSolo()).toBeTrue();
  });

  it('el selector de patrocinador no aparece en el HTML para un PATROCINADOR puro', () => {
    const selects: NodeListOf<HTMLSelectElement> =
      fixture.nativeElement.querySelectorAll('select');
    const idsFormulario = Array.from(selects).map((s) => s.getAttribute('formcontrolname'));
    expect(idsFormulario).not.toContain('patrocinadorId');
  });
});
