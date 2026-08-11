import { Routes } from '@angular/router';

/** RF-37: consulta de métricas de transmisión por período. */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/metricas-transmision/metricas-transmision.component').then(
        (m) => m.MetricasTransmisionComponent
      )
  }
];
