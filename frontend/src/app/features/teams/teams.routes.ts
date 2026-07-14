import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/team-list/team-list.component').then((m) => m.TeamListComponent)
  },
  {
    path: 'nuevo',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/team-form/team-form.component').then((m) => m.TeamFormComponent)
  },
  {
    path: ':equipoId/editar',
    loadComponent: () =>
      import('./pages/team-form/team-form.component').then((m) => m.TeamFormComponent)
  },
  {
    path: ':equipoId/plantilla',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/team-roster/team-roster.component').then((m) => m.TeamRosterComponent)
  }
];
