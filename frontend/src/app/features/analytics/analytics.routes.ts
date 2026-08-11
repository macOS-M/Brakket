import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/analytics-dashboard/analytics-dashboard.component').then((m) => m.AnalyticsDashboardComponent)
  },
  {
    // RF-40: el termómetro va aparte del panel de análisis de RF-39, que es una
    // herramienta de administración; a este llegan también comisionados y
    // patrocinadores.
    path: 'termometro',
    loadComponent: () =>
      import('./pages/termometro-page/termometro-page.component').then((m) => m.TermometroPageComponent)
  }
];
