import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'callback',
    loadComponent: () =>
      import('./pages/callback/callback.component').then((m) => m.CallbackComponent)
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' }
];
