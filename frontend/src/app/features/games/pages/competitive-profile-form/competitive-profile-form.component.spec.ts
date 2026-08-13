import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { CompetitiveProfileFormComponent } from './competitive-profile-form.component';

/**
 * Configurar el perfil competitivo de un juego (RF-21).
 *
 * <p>Lo que se protege acá es que el formulario <b>no traiga la plantilla
 * precargada</b>. Con 1/1 por defecto —una combinación válida— aceptar sin
 * tocar nada guardaba un perfil que parecía configurado y después bloqueaba los
 * torneos del juego: el de Valorant quedó exigiendo equipos de 1 a 1.</p>
 */
describe('CompetitiveProfileFormComponent', () => {
  let component: CompetitiveProfileFormComponent;
  let fixture: ComponentFixture<CompetitiveProfileFormComponent>;
  let httpMock: HttpTestingController;

  /** El formulario pide juego, catálogos y perfil en paralelo al iniciar. */
  function responderCarga(conPerfil = false): void {
    httpMock.expectOne(`${environment.apiUrl}/games/59`)
      .flush({ id: 59, nombre: 'Fortnite', genero: 'Shooter', activo: true });
    httpMock.expectOne(`${environment.apiUrl}/competitive-catalogs/formats`)
      .flush([{ id: 1, nombre: 'ELIMINACION_DIRECTA', obligatorio: false }]);
    httpMock.expectOne(`${environment.apiUrl}/competitive-catalogs/statistics`)
      .flush([
        { id: 1, nombre: 'PARTIDAS_JUGADAS', obligatorio: true },
        { id: 2, nombre: 'VICTORIAS', obligatorio: true },
        { id: 9, nombre: 'PUNTOS_POR_KILL', obligatorio: false }
      ]);
    const perfil = httpMock.expectOne(`${environment.apiUrl}/competitive-profiles/game/59`);
    if (conPerfil) {
      perfil.flush({
        id: 4, juegoId: 59, modalidad: 'EQUIPOS', plantillaMinima: 1, plantillaMaxima: 4,
        formatosIds: [1], estadisticasIds: [1, 2], activo: true, mensaje: null
      });
    } else {
      // 404 es el caso normal: el juego todavía no tiene perfil.
      perfil.flush({ message: 'no hay perfil' }, { status: 404, statusText: 'Not Found' });
    }
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompetitiveProfileFormComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '59' } } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CompetitiveProfileFormComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('arranca sin plantilla precargada', () => {
    responderCarga();

    expect(component.form.controls.plantillaMinima.value).toBeNull();
    expect(component.form.controls.plantillaMaxima.value).toBeNull();
    // Sin decidirla, el formulario no se puede enviar.
    expect(component.form.invalid).toBeTrue();
  });

  it('preselecciona las estadisticas obligatorias', () => {
    responderCarga();

    // El backend las exige para activar el perfil; pedirle al usuario que las
    // tilde a mano es trabajo sin alternativa.
    expect(component.form.controls.estadisticasIds.value).toEqual([1, 2]);
  });

  it('no guarda si falta la plantilla', () => {
    responderCarga();
    component.form.controls.formatosIds.setValue([1]);

    component.guardar();

    httpMock.expectNone(`${environment.apiUrl}/competitive-profiles`);
    expect(component.error()).toContain('Completa los campos obligatorios');
  });

  it('rechaza un maximo menor que el minimo antes de llamar al backend', () => {
    responderCarga();
    component.form.patchValue({ plantillaMinima: 5, plantillaMaxima: 2 });
    component.form.controls.formatosIds.setValue([1]);

    component.guardar();

    httpMock.expectNone(`${environment.apiUrl}/competitive-profiles`);
    expect(component.error()).toContain('no puede ser mayor al máximo');
  });

  it('carga la plantilla del perfil existente al editar', () => {
    responderCarga(true);

    expect(component.form.controls.plantillaMinima.value).toBe(1);
    expect(component.form.controls.plantillaMaxima.value).toBe(4);
  });
});
