import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/game-list/game-list.component').then((m) => m.GameListComponent)
  },
  {
    path: 'nuevo',
    loadComponent: () =>
      import('./pages/game-form/game-form.component').then((m) => m.GameFormComponent)
  },
  {
    path: ':id/editar',
    loadComponent: () =>
      import('./pages/game-form/game-form.component').then((m) => m.GameFormComponent)
  }
];
