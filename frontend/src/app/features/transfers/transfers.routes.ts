import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/transfer-list/transfer-list.component').then((m) => m.TransferListComponent)
  },
  {
    path: 'nueva',
    loadComponent: () =>
      import('./pages/transfer-form/transfer-form.component').then((m) => m.TransferFormComponent)
  }
];
