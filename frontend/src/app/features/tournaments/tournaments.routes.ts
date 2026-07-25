import { Routes } from '@angular/router';

/** Torneos públicos: navegables sin sesión; se actúa (crear/inscribir) con login. */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/tournament-list/tournament-list.component').then((m) => m.TournamentListComponent)
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/tournament-detail/tournament-detail.component').then((m) => m.TournamentDetailComponent)
  }
];
