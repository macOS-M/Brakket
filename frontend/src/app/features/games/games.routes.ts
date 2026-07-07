import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/game-list/game-list.component').then((m) => m.GameListComponent)
  }
];
