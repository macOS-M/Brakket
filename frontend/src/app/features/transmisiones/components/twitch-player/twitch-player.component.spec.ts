import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TwitchPlayerComponent } from './twitch-player.component';

describe('TwitchPlayerComponent', () => {
  let fixture: ComponentFixture<TwitchPlayerComponent>;
  let component: TwitchPlayerComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TwitchPlayerComponent] }).compileComponents();
    fixture = TestBed.createComponent(TwitchPlayerComponent);
    component = fixture.componentInstance;
  });

  function srcDelIframe(): string {
    return fixture.nativeElement.querySelector('iframe').src;
  }

  it('incluye el parámetro parent con el hostname actual (exigido por el embed)', () => {
    component.canal = 'brakketcenfotec';
    fixture.detectChanges();

    expect(srcDelIframe()).toContain('channel=brakketcenfotec');
    expect(srcDelIframe()).toContain(`parent=${window.location.hostname}`);
    expect(srcDelIframe()).toContain('muted=true');
  });

  it('prioriza el VOD sobre el canal cuando vienen ambos', () => {
    component.canal = 'brakketcenfotec';
    component.videoId = 'v123';
    fixture.detectChanges();

    expect(srcDelIframe()).toContain('video=v123');
    expect(srcDelIframe()).not.toContain('channel=');
  });
});
