import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/league-list/league-list.component').then((m) => m.LeagueListComponent)
  }
];
