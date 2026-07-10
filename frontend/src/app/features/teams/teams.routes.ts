import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/team-list/team-list.component').then((m) => m.TeamListComponent)
  },
  {
    path: ':equipoId/plantilla',
    loadComponent: () =>
      import('./pages/team-roster/team-roster.component').then((m) => m.TeamRosterComponent)
  }
];
