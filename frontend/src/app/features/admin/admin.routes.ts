import { Routes } from '@angular/router';

export const routes: Routes = [
  // Panel global (RF-49): landing de administración.
  {
    path: '',
    loadComponent: () =>
      import('./pages/global-panel/global-panel.component').then((m) => m.GlobalPanelComponent)
  },
  // Gestión de roles y permisos (RF-19).
  {
    path: 'roles',
    loadComponent: () =>
      import('./pages/admin-panel/admin-panel.component').then((m) => m.AdminPanelComponent)
  }
];
