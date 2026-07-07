import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProgressionViewComponent } from './progression-view.component';

describe('ProgressionViewComponent', () => {
  let component: ProgressionViewComponent;
  let fixture: ComponentFixture<ProgressionViewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProgressionViewComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ProgressionViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
