import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { DisputeListComponent } from './dispute-list.component';
import { MiDisputa } from '../../../../models/disputa.model';

describe('DisputeListComponent', () => {
  let component: DisputeListComponent;
  let fixture: ComponentFixture<DisputeListComponent>;
  let http: HttpTestingController;

  const disputa: MiDisputa = {
    disputaId: 1,
    torneoId: 7,
    torneoNombre: 'Copa Relampago (Demo)',
    partidaId: 200,
    equipoANombre: 'Fenix Demo',
    equipoBNombre: 'Lobos Demo',
    motivo: 'El marcador no coincide',
    estado: 'PENDIENTE',
    levantadaPorNombre: 'Ana Fenix',
    fechaCreacion: '2026-08-10T18:00:00'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DisputeListComponent],
      // El componente consulta al entrar, así que necesita HttpClient; y
      // navega al bracket del torneo, así que necesita un Router.
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(DisputeListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  it('should create', () => {
    fixture.detectChanges();
    http.expectOne((r) => r.url.endsWith('/disputas/mias')).flush([]);

    expect(component).toBeTruthy();
  });

  it('carga las disputas del usuario al entrar', () => {
    fixture.detectChanges();

    const peticion = http.expectOne((r) => r.url.endsWith('/disputas/mias'));
    expect(peticion.request.method).toBe('GET');
    peticion.flush([disputa]);

    expect(component.cargando()).toBeFalse();
    expect(component.disputas().length).toBe(1);
    expect(component.disputas()[0].torneoNombre).toBe('Copa Relampago (Demo)');
  });

  it('muestra un mensaje si la consulta falla', () => {
    fixture.detectChanges();

    http.expectOne((r) => r.url.endsWith('/disputas/mias'))
      .flush({ message: 'No autorizado' }, { status: 403, statusText: 'Forbidden' });

    expect(component.cargando()).toBeFalse();
    expect(component.error()).toBe('No autorizado');
  });
});
