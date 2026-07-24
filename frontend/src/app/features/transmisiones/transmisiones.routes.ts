import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/transmisiones-page/transmisiones-page.component').then(
        (m) => m.TransmisionesPageComponent
      )
  }
];
