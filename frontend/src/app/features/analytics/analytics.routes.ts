import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/analytics-dashboard/analytics-dashboard.component').then((m) => m.AnalyticsDashboardComponent)
  }
];
