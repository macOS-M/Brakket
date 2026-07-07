import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/admin-panel/admin-panel.component').then((m) => m.AdminPanelComponent)
  }
];
