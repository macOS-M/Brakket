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
  },
  {
    path: 'asociaciones',
    loadComponent: () =>
      import('./pages/association-list/association-list.component').then((m) => m.AssociationListComponent)
  },
  {
    path: 'asociaciones/nuevo',
    loadComponent: () =>
      import('./pages/association-form/association-form.component').then((m) => m.AssociationFormComponent)
  },
  {
    path: 'asociaciones/:patrocinioId/espacios',
    loadComponent: () =>
      import('./pages/espacio-list/espacio-list.component').then((m) => m.EspacioListComponent)
  },
  {
    path: 'asociaciones/:patrocinioId/espacios/nuevo',
    loadComponent: () =>
      import('./pages/espacio-form/espacio-form.component').then((m) => m.EspacioFormComponent)
  }
];
