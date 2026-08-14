import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { ProgressionViewComponent } from './progression-view.component';

describe('ProgressionViewComponent', () => {
  let component: ProgressionViewComponent;
  let fixture: ComponentFixture<ProgressionViewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProgressionViewComponent],
      // El componente consulta la progresión al iniciar; sin cliente HTTP de
      // pruebas la inyección de ProgressionService falla antes de crearlo.
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(ProgressionViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
