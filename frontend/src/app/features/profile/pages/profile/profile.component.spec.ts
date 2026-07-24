import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ProfileComponent } from './profile.component';
import { ApiService } from '../../../../core/services/api.service';
import { AuthService } from '../../../../core/services/auth.service';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: ApiService,
          useValue: {
            get: () => of([])
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /** Fecha local YYYY-MM-DD (toISOString corre el día en husos negativos). */
  const fechaLocal = (fecha: Date): string => {
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${fecha.getFullYear()}-${mes}-${dia}`;
  };

  it('calcula la edad a partir de la fecha de nacimiento', () => {
    const hace20 = new Date();
    hace20.setFullYear(hace20.getFullYear() - 20);
    component.profileForm.controls.fechaNacimiento.setValue(fechaLocal(hace20));

    expect(component.edad).toBe(20);
  });

  it('sin fecha de nacimiento no muestra edad', () => {
    component.profileForm.controls.fechaNacimiento.setValue('');

    expect(component.edad).toBeNull();
  });

  it('el tope del datepicker deja fuera a los menores de 13', () => {
    const hace13 = new Date();
    hace13.setFullYear(hace13.getFullYear() - 13);

    expect(component.maxFechaNacimiento).toBe(fechaLocal(hace13));
  });

  it('cuenta los ajustes personales completos', () => {
    expect(component.camposPersonalesCompletos).toBe(0);

    component.profileForm.patchValue({
      nombreCompleto: 'Ana Rojas',
      telefono: '+50688887777',
      pais: 'Costa Rica'
    });

    expect(component.camposPersonalesCompletos).toBe(3);
  });

  it('detectar zona horaria toma la del navegador', () => {
    component.detectarZonaHoraria();

    expect(component.profileForm.controls.zonaHoraria.value)
      .toBe(Intl.DateTimeFormat().resolvedOptions().timeZone);
  });

  it('guarda los ajustes personales y manda null en los campos vacios', () => {
    const authService = TestBed.inject(AuthService);
    spyOn(authService, 'updateCurrentUser').and.returnValue(of({ authenticated: true }));

    component.profileForm.patchValue({
      nombre: 'Ana',
      nombreCompleto: 'Ana Sofia Rojas',
      telefono: '+506 8888 7777',
      pais: 'Costa Rica'
    });
    component.profileForm.markAsDirty();
    component.saveProfile();

    expect(authService.updateCurrentUser).toHaveBeenCalledWith(jasmine.objectContaining({
      nombreCompleto: 'Ana Sofia Rojas',
      telefono: '+506 8888 7777',
      pais: 'Costa Rica',
      ciudad: null,
      direccion: null
    }));
  });
});
