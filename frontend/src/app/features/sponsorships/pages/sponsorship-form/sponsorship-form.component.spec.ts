import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { SponsorshipFormComponent } from './sponsorship-form.component';
import { SponsorshipsService } from '../../services/sponsorships.service';

describe('SponsorshipFormComponent', () => {
  let component: SponsorshipFormComponent;
  let fixture: ComponentFixture<SponsorshipFormComponent>;

  beforeEach(async () => {
    const sponsorshipsServiceMock = {
      crear: () => of({}),
      editar: () => of({}),
      obtener: () => of({})
    };

    await TestBed.configureTestingModule({
      imports: [SponsorshipFormComponent],
      providers: [
        { provide: SponsorshipsService, useValue: sponsorshipsServiceMock },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => null } } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SponsorshipFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should be invalid when required fields are empty', () => {
    expect(component.form.invalid).toBeTrue();
  });
});
