import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { GameFormComponent } from './game-form.component';

describe('GameFormComponent', () => {
  let component: GameFormComponent;
  let fixture: ComponentFixture<GameFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GameFormComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => null } } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GameFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start in "crear" mode when there is no id param', () => {
    expect(component.editando()).toBeFalse();
  });

  it('should mark the form invalid when required fields are empty', () => {
    component.form.setValue({ nombre: '', genero: '', descripcion: '', imagenUrl: '' });
    expect(component.form.invalid).toBeTrue();
  });

  it('elegir un resultado externo precarga nombre, género y portada', () => {
    component.elegir({
      nombre: 'Valorant',
      genero: 'Shooter',
      imagenUrl: 'https://media.rawg.io/valorant.jpg'
    });

    expect(component.form.value.nombre).toBe('Valorant');
    expect(component.form.value.genero).toBe('Shooter');
    expect(component.form.value.imagenUrl).toBe('https://media.rawg.io/valorant.jpg');
    expect(component.resultados()).toBeNull();
  });
});
