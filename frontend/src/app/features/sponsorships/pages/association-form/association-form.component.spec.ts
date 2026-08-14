import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AssociationFormComponent } from './association-form.component';
import { PatrociniosService } from '../../services/patrocinios.service';
import { SponsorshipsService } from '../../services/sponsorships.service';
import { LeaguesService } from '../../../leagues/services/leagues.service';
import { TournamentsService } from '../../../tournaments/services/tournaments.service';

describe('AssociationFormComponent', () => {
  let component: AssociationFormComponent;
  let fixture: ComponentFixture<AssociationFormComponent>;
  let patrociniosServiceSpy: jasmine.SpyObj<PatrociniosService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    patrociniosServiceSpy = jasmine.createSpyObj('PatrociniosService', ['crear']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    const sponsorshipsServiceSpy = jasmine.createSpyObj('SponsorshipsService', ['listar']);
    sponsorshipsServiceSpy.listar.and.returnValue(of([]));

    const leaguesServiceSpy = jasmine.createSpyObj('LeaguesService', ['list', 'listSeasons']);
    leaguesServiceSpy.list.and.returnValue(of([]));
    leaguesServiceSpy.listSeasons.and.returnValue(of([]));

    const tournamentsServiceSpy = jasmine.createSpyObj('TournamentsService', ['listar']);
    tournamentsServiceSpy.listar.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [AssociationFormComponent, ReactiveFormsModule],
      providers: [
        { provide: PatrociniosService, useValue: patrociniosServiceSpy },
        { provide: SponsorshipsService, useValue: sponsorshipsServiceSpy },
        { provide: LeaguesService, useValue: leaguesServiceSpy },
        { provide: TournamentsService, useValue: tournamentsServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: {} }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AssociationFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('el formulario es invalido cuando esta vacio', () => {
    expect(component.form.invalid).toBeTrue();
  });

  it('el formulario es valido cuando se completan los campos requeridos con alcance TORNEO', () => {
    component.form.patchValue({
      patrocinadorId: 1,
      alcance: 'TORNEO',
      torneoId: 12,
      fechaInicio: '2026-08-05',
      fechaFin: '2026-12-31'
    });
    expect(component.form.valid).toBeTrue();
  });

  it('no llama al servicio de crear si el formulario es invalido', () => {
    component.guardar();
    expect(patrociniosServiceSpy.crear).not.toHaveBeenCalled();
  });

  it('llama al servicio de crear con los datos correctos y navega al guardar', () => {
    patrociniosServiceSpy.crear.and.returnValue(of({} as any));

    component.form.patchValue({
      patrocinadorId: 1,
      alcance: 'TORNEO',
      torneoId: 12,
      condiciones: 'Prueba',
      fechaInicio: '2026-08-05',
      fechaFin: '2026-12-31'
    });

    component.guardar();

    expect(patrociniosServiceSpy.crear).toHaveBeenCalledWith(jasmine.objectContaining({
      patrocinadorId: 1,
      torneoId: 12,
      ligaId: null,
      temporadaId: null
    }));
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/sponsorships/asociaciones']);
  });

  it('muestra un mensaje de error si el servicio de crear falla', () => {
    patrociniosServiceSpy.crear.and.returnValue(throwError(() => ({ error: { message: 'Error de prueba' } })));

    component.form.patchValue({
      patrocinadorId: 1,
      alcance: 'TORNEO',
      torneoId: 12,
      fechaInicio: '2026-08-05',
      fechaFin: '2026-12-31'
    });

    component.guardar();

    expect(component.error()).toBe('Error de prueba');
    expect(component.guardando()).toBeFalse();
  });
});
