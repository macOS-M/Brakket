import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TwitchPanelComponent } from './twitch-panel.component';

describe('TwitchPanelComponent', () => {
  let component: TwitchPanelComponent;
  let fixture: ComponentFixture<TwitchPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TwitchPanelComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TwitchPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
