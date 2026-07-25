import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/sponsorship-list/sponsorship-list.component').then((m) => m.SponsorshipListComponent)
  },
  {
    path: 'nuevo',
    loadComponent: () =>
      import('./pages/sponsorship-form/sponsorship-form.component').then((m) => m.SponsorshipFormComponent)
  },
  {
    path: ':id/editar',
    loadComponent: () =>
      import('./pages/sponsorship-form/sponsorship-form.component').then((m) => m.SponsorshipFormComponent)
  }
];
