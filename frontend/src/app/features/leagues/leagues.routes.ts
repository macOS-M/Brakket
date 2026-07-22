import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';

/**
 * Modelo abierto de organizadores: la lista y el detalle son públicos;
 * crear exige solo sesión (quien crea queda como comisionado de SU liga)
 * y editar valida propiedad en el backend.
 */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/league-list/league-list.component').then((m) => m.LeagueListComponent)
  },
  {
    path: 'nuevo',
    canActivate: [authGuard],
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
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/league-form/league-form.component').then((m) => m.LeagueFormComponent)
  }
];
