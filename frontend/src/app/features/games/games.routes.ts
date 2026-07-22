import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';

/**
 * El catalogo es navegable sin sesion (lectura publica); crear, editar y
 * configurar perfiles competitivos exigen ADMIN o COMISIONADO (RF-20/RF-21).
 */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/game-list/game-list.component').then((m) => m.GameListComponent)
  },
  {
    path: 'nuevo',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN', 'COMISIONADO'] },
    loadComponent: () =>
      import('./pages/game-form/game-form.component').then((m) => m.GameFormComponent)
  },
  {
    path: ':id/editar',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN', 'COMISIONADO'] },
    loadComponent: () =>
      import('./pages/game-form/game-form.component').then((m) => m.GameFormComponent)
  },
  {
    path: ':juegoId/perfil-competitivo',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN', 'COMISIONADO'] },
    loadComponent: () =>
      import('./pages/competitive-profile-form/competitive-profile-form.component')
        .then((m) => m.CompetitiveProfileFormComponent)
  }
];
