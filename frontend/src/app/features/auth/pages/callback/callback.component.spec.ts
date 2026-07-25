import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { CallbackComponent } from './callback.component';

describe('CallbackComponent', () => {
  let component: CallbackComponent;
  let fixture: ComponentFixture<CallbackComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CallbackComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({}) } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CallbackComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
