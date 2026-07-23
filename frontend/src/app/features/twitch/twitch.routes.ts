import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./pages/twitch-panel/twitch-panel.component').then((m) => m.TwitchPanelComponent)
  }
];
