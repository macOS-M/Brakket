import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent)
  },
  {
    path: '',
    loadComponent: () =>
      import('./shared/components/layout/layout.component').then((m) => m.LayoutComponent),
    children: [
      {
        path: '',
        loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent)
      },
      {
        path: 'juegos',
        loadComponent: () =>
          import('./features/juegos/juegos.component').then((m) => m.JuegosComponent)
      },
      {
        path: 'perfil',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/perfil/perfil.component').then((m) => m.PerfilComponent)
      },
      {
        path: 'equipos',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/equipos/equipos.component').then((m) => m.EquiposComponent)
      },
      {
        path: 'ligas',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/ligas/ligas.component').then((m) => m.LigasComponent)
      },
      {
        path: 'torneos',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/torneos/torneos.component').then((m) => m.TorneosComponent)
      },
      {
        path: 'disputas',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/disputas/disputas.component').then((m) => m.DisputasComponent)
      },
      {
        path: 'twitch',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/twitch/twitch.component').then((m) => m.TwitchComponent)
      },
      {
        path: 'patrocinios',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/patrocinios/patrocinios.component').then((m) => m.PatrociniosComponent)
      },
      {
        path: 'estadisticas',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/estadisticas/estadisticas.component').then(
            (m) => m.EstadisticasComponent
          )
      },
      {
        path: 'admin',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/admin/admin.component').then((m) => m.AdminComponent)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
