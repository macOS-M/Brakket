import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { LeagueListComponent } from './league-list.component';

describe('LeagueListComponent', () => {
  let component: LeagueListComponent;
  let fixture: ComponentFixture<LeagueListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LeagueListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(LeagueListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
