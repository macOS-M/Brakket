import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SponsorshipListComponent } from './sponsorship-list.component';

describe('SponsorshipListComponent', () => {
  let component: SponsorshipListComponent;
  let fixture: ComponentFixture<SponsorshipListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SponsorshipListComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(SponsorshipListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
