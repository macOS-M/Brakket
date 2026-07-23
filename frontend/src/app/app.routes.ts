import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/landing/pages/landing-institucional/landing-institucional.component')
      .then((m) => m.LandingInstitucionalComponent)
  },
  {
    path: 'producto',
    loadComponent: () => import('./features/landing/pages/landing-producto/landing-producto.component')
      .then((m) => m.LandingProductoComponent)
  },
  {
    path: 'team-profile/:equipoId',
    loadComponent: () => import('./features/teams/pages/team-public-profile/team-public-profile.component')
      .then((m) => m.TeamPublicProfileComponent)
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.routes)
  },
  // El backend redirige a /auth/login y /auth/callback; /login se conserva por
  // compatibilidad con el guard y el logout, que navegan a /login.
  { path: 'login', redirectTo: 'auth/login', pathMatch: 'full' },
  {
    path: '',
    loadComponent: () => import('./layout/layout.component').then((m) => m.LayoutComponent),
    children: [
      {
        path: 'inicio',
        loadChildren: () => import('./features/home/home.routes').then((m) => m.routes)
      },
      {
        path: 'games',
        loadChildren: () => import('./features/games/games.routes').then((m) => m.routes)
      },
      {
        // Lectura pública; los guards de crear/editar viven en sus rutas hijas.
        path: 'leagues',
        loadChildren: () => import('./features/leagues/leagues.routes').then((m) => m.routes)
      },
      {
        // Lectura pública de torneos; crear/inscribir piden sesión al actuar.
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
        loadChildren: () => import('./features/teams/teams.routes').then((m) => m.routes)
      },
      {
        path: 'transfers',
        canActivate: [authGuard],
        loadChildren: () => import('./features/transfers/transfers.routes').then((m) => m.routes)
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
        // Analítica de audiencia: la consumen quienes gestionan competencias
        // (RF-49) y el patrocinador a través de su panel comercial (RF-44).
        path: 'analytics',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['ADMIN', 'COMISIONADO', 'PATROCINADOR'] },
        loadChildren: () => import('./features/analytics/analytics.routes').then((m) => m.routes)
      },
      {
        // Gestión de patrocinios: administrador o comisionado (RF-42/RF-43).
        path: 'sponsorships',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['ADMIN', 'COMISIONADO'] },
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
