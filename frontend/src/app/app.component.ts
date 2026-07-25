import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { ThemeService } from './core/services/theme.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'brakket-frontend';

  // Se instancia en el arranque para que la preferencia de tema se aplique
  // en toda la app y no solo donde vive el sidebar: el login y el callback
  // quedan fuera del layout, y sin esto ignorarian la eleccion del usuario.
  private readonly theme = inject(ThemeService);
}
