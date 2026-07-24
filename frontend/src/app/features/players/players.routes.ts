import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: ':jugadorId/historial',
    loadComponent: () =>
      import('./pages/player-history/player-history.component').then((m) => m.PlayerHistoryComponent)
  }
];
