import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { AnalyticsDashboardComponent } from './analytics-dashboard.component';

describe('AnalyticsDashboardComponent', () => {
  let component: AnalyticsDashboardComponent;
  let fixture: ComponentFixture<AnalyticsDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalyticsDashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyticsDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('exige el ID de la transmisión antes de analizar', () => {
    component.transmisionId = null;
    component.mensajesTexto = 'gg';
    component.analizar();
    expect(component.error()).toContain('transmisión');
  });

  it('exige al menos un mensaje de chat', () => {
    component.transmisionId = 1;
    component.mensajesTexto = '   ';
    component.analizar();
    expect(component.error()).toContain('mensaje');
  });
});
