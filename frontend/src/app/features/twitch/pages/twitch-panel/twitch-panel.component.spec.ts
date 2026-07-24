import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TwitchPanelComponent } from './twitch-panel.component';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../../../environments/environment';

describe('TwitchPanelComponent', () => {
  let component: TwitchPanelComponent;
  let fixture: ComponentFixture<TwitchPanelComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TwitchPanelComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(TwitchPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    http = TestBed.inject(HttpTestingController);
  });

  it('should create', () => {
    http.expectOne(`${environment.apiUrl}/twitch`).flush({
      id: null, twitchUsuarioId: null, loginCanal: null, nombreMostrado: null,
      urlCanal: null, estado: 'SIN_CONFIGURAR', activo: false, ultimoError: null,
      ultimaValidacion: null, credencialesConfiguradas: false
    });
    expect(component).toBeTruthy();
  });
});
