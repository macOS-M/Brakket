import { Component, Input, computed, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { inject } from '@angular/core';

/**
 * Player embebido de Twitch (canal en vivo o VOD).
 *
 * <p>El parámetro obligatorio `parent` se resuelve con el hostname actual:
 * así queda parametrizado por entorno sin configuración extra (localhost en
 * dev, el dominio real en producción). El chat embebido está fuera de
 * alcance: el hueco lo reserva el layout del hero, no este componente.</p>
 */
@Component({
  selector: 'app-twitch-player',
  standalone: true,
  template: `
    <div class="player">
      <iframe
        [src]="urlSegura()"
        [title]="titulo"
        allowfullscreen
        allow="autoplay; fullscreen; picture-in-picture"
      ></iframe>
    </div>
  `,
  styles: `
    .player {
      position: relative;
      width: 100%;
      aspect-ratio: 16 / 9;
      border-radius: 12px;
      overflow: hidden;
      background: #000;
    }
    iframe {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      border: 0;
    }
  `
})
export class TwitchPlayerComponent {
  private readonly sanitizer = inject(DomSanitizer);

  private readonly _canal = signal<string | null>(null);
  private readonly _videoId = signal<string | null>(null);

  /** Canal en vivo a reproducir. */
  @Input() set canal(valor: string | null | undefined) {
    this._canal.set(valor ?? null);
  }

  /** VOD a reproducir; tiene prioridad sobre el canal si vienen ambos. */
  @Input() set videoId(valor: string | null | undefined) {
    this._videoId.set(valor ?? null);
  }

  @Input() titulo = 'Reproductor de Twitch';

  readonly urlSegura = computed<SafeResourceUrl>(() => {
    const params = new URLSearchParams();
    const videoId = this._videoId();
    if (videoId) {
      params.set('video', videoId);
    } else {
      params.set('channel', this._canal() ?? '');
    }
    params.set('parent', window.location.hostname);
    // Autoplay silenciado; con prefers-reduced-motion no arranca solo.
    const reducirMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    params.set('autoplay', reducirMovimiento ? 'false' : 'true');
    params.set('muted', 'true');
    return this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://player.twitch.tv/?${params.toString()}`
    );
  });
}
