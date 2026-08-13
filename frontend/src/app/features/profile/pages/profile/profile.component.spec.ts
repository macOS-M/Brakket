import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ProfileComponent } from './profile.component';
import { ApiService } from '../../../../core/services/api.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ahoraCostaRica, isoDeFechaLocal } from '../../../../shared/utils/hora-costa-rica';

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

  /**
   * Se parte del reloj de Costa Rica, igual que el componente. Con `new Date()`
   * la prueba usaba la hora de la máquina: pasaba acá y fallaba en CI, que corre
   * en UTC, porque después de las 18:00 el día ya cambió allá y no acá.
   */
  const hace = (anios: number): string => {
    const fecha = ahoraCostaRica();
    fecha.setFullYear(fecha.getFullYear() - anios);
    return isoDeFechaLocal(fecha);
  };

  it('calcula la edad a partir de la fecha de nacimiento', () => {
    component.profileForm.controls.fechaNacimiento.setValue(hace(20));

    expect(component.edad).toBe(20);
  });

  it('sin fecha de nacimiento no muestra edad', () => {
    component.profileForm.controls.fechaNacimiento.setValue('');

    expect(component.edad).toBeNull();
  });

  it('el tope del datepicker deja fuera a los menores de 13', () => {
    expect(component.maxFechaNacimiento).toBe(hace(13));
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
