import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DisputeListComponent } from './dispute-list.component';

describe('DisputeListComponent', () => {
  let component: DisputeListComponent;
  let fixture: ComponentFixture<DisputeListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DisputeListComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(DisputeListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
