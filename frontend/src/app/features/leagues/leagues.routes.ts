import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/league-list/league-list.component').then((m) => m.LeagueListComponent)
  },
  {
    path: 'nuevo',
    loadComponent: () =>
      import('./pages/league-form/league-form.component').then((m) => m.LeagueFormComponent)
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/league-detail/league-detail.component').then((m) => m.LeagueDetailComponent)
  },
  {
    path: ':id/editar',
    loadComponent: () =>
      import('./pages/league-form/league-form.component').then((m) => m.LeagueFormComponent)
  }
];
