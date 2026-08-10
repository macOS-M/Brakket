import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { GlobalPanelComponent } from './global-panel.component';

describe('GlobalPanelComponent', () => {
  let component: GlobalPanelComponent;
  let fixture: ComponentFixture<GlobalPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GlobalPanelComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(GlobalPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('rotula al actor nulo como "Sistema"', () => {
    const label = component.actor({
      id: 1, fecha: '', accion: 'X', entidad: 'juego', entidadId: 1,
      actorNombre: null, actorCorreo: null
    });
    expect(label).toBe('Sistema');
  });
});
