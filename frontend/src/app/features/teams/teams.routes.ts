import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/team-list/team-list.component').then((m) => m.TeamListComponent)
  },
  {
    path: 'nuevo',
    loadComponent: () =>
      import('./pages/team-form/team-form.component').then((m) => m.TeamFormComponent)
  },
  {
    path: 'invitaciones',
    loadComponent: () =>
      import('./pages/my-invitations/my-invitations.component').then((m) => m.MyInvitationsComponent)
  },
  {
    path: ':equipoId/plantilla',
    loadComponent: () =>
      import('./pages/team-roster/team-roster.component').then((m) => m.TeamRosterComponent)
  }
];
