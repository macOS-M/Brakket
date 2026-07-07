import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/notification-list/notification-list.component').then((m) => m.NotificationListComponent)
  }
];
