import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/tournament-list/tournament-list.component').then((m) => m.TournamentListComponent)
  }
];
