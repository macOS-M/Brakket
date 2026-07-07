import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.routes)
  },
  {
    path: '',
    loadComponent: () => import('./layout/layout.component').then((m) => m.LayoutComponent),
    children: [
      {
        path: '',
        loadChildren: () => import('./features/home/home.routes').then((m) => m.routes)
      },
      {
        path: 'games',
        loadChildren: () => import('./features/games/games.routes').then((m) => m.routes)
      },
      {
        path: 'leagues',
        loadChildren: () => import('./features/leagues/leagues.routes').then((m) => m.routes)
      },
      {
        path: 'tournaments',
        loadChildren: () =>
          import('./features/tournaments/tournaments.routes').then((m) => m.routes)
      },
      {
        path: 'profile',
        canActivate: [authGuard],
        loadChildren: () => import('./features/profile/profile.routes').then((m) => m.routes)
      },
      {
        path: 'teams',
        canActivate: [authGuard],
        loadChildren: () => import('./features/teams/teams.routes').then((m) => m.routes)
      },
      {
        path: 'disputes',
        canActivate: [authGuard],
        loadChildren: () => import('./features/disputes/disputes.routes').then((m) => m.routes)
      },
      {
        path: 'twitch',
        canActivate: [authGuard],
        loadChildren: () => import('./features/twitch/twitch.routes').then((m) => m.routes)
      },
      {
        path: 'analytics',
        canActivate: [authGuard],
        loadChildren: () => import('./features/analytics/analytics.routes').then((m) => m.routes)
      },
      {
        path: 'sponsorships',
        canActivate: [authGuard],
        loadChildren: () =>
          import('./features/sponsorships/sponsorships.routes').then((m) => m.routes)
      },
      {
        path: 'notifications',
        canActivate: [authGuard],
        loadChildren: () =>
          import('./features/notifications/notifications.routes').then((m) => m.routes)
      },
      {
        path: 'statistics',
        canActivate: [authGuard],
        loadChildren: () => import('./features/statistics/statistics.routes').then((m) => m.routes)
      },
      {
        path: 'progression',
        canActivate: [authGuard],
        loadChildren: () =>
          import('./features/progression/progression.routes').then((m) => m.routes)
      },
      {
        path: 'admin',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['ADMIN'] },
        loadChildren: () => import('./features/admin/admin.routes').then((m) => m.routes)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
