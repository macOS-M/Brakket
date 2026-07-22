import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';

/**
 * La lista y el detalle son navegables sin sesion (lectura publica); crear y
 * configurar exigen sesion y el rol de quien gestiona ligas (RF-22).
 */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/league-list/league-list.component').then((m) => m.LeagueListComponent)
  },
  {
    path: 'nuevo',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN', 'COMISIONADO'] },
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
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN', 'COMISIONADO'] },
    loadComponent: () =>
      import('./pages/league-form/league-form.component').then((m) => m.LeagueFormComponent)
  }
];
