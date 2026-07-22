import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';

/**
 * El catalogo es navegable sin sesion (lectura publica). La gestion del
 * catalogo (alta manual de respaldo, editar, perfil competitivo) es solo
 * de ADMIN: en el modelo abierto los juegos entran por la API y los
 * usuarios crean ligas/torneos, no juegos a mano.
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
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./pages/game-form/game-form.component').then((m) => m.GameFormComponent)
  },
  {
    // Hub público del juego (banner, resumen, torneos). Sin guard.
    path: ':id',
    loadComponent: () =>
      import('./pages/game-hub/game-hub.component').then((m) => m.GameHubComponent)
  },
  {
    path: ':id/editar',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./pages/game-form/game-form.component').then((m) => m.GameFormComponent)
  },
  {
    path: ':juegoId/perfil-competitivo',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./pages/competitive-profile-form/competitive-profile-form.component')
        .then((m) => m.CompetitiveProfileFormComponent)
  }
];
