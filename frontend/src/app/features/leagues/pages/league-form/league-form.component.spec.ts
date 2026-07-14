import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { LeagueFormComponent } from './league-form.component';

describe('LeagueFormComponent', () => {
  let component: LeagueFormComponent;
  let fixture: ComponentFixture<LeagueFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LeagueFormComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(LeagueFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('el formulario es inválido cuando está vacío', () => {
    expect(component.form.valid).toBeFalse();
  });
});
