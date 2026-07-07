import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/statistics-view/statistics-view.component').then((m) => m.StatisticsViewComponent)
  }
];
