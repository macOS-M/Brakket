import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/dispute-list/dispute-list.component').then((m) => m.DisputeListComponent)
  }
];
