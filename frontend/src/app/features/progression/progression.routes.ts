import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/progression-view/progression-view.component').then((m) => m.ProgressionViewComponent)
  }
];
