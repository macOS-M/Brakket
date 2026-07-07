import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/sponsorship-list/sponsorship-list.component').then((m) => m.SponsorshipListComponent)
  }
];
