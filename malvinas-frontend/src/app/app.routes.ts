import { Routes } from '@angular/router';
import { authGuard, roleGuard, homeGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: '',
    loadComponent: () => import('./layout/app.layout.component').then(m => m.AppLayoutComponent),
    canActivate: [authGuard],
    children: [
      // Root '' always redirects to the role's home page
      {
        path: '',
        pathMatch: 'full',
        canActivate: [homeGuard],
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'dashboard',
        canActivate: [roleGuard],
        data: { roles: ['ADM', 'SUP'] },
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'vehiculos',
        canActivate: [roleGuard],
        data: { roles: ['ADM', 'SUP'] },
        loadComponent: () => import('./features/vehiculos/vehiculos.component').then(m => m.VehiculosComponent)
      },
      {
        path: 'cargas',
        canActivate: [roleGuard],
        data: { roles: ['ADM', 'SUP', 'MOV'] },
        loadComponent: () => import('./features/cargas/cargas.component').then(m => m.CargasComponent)
      },
      {
        path: 'personal',
        canActivate: [roleGuard],
        data: { roles: ['ADM'] },
        loadComponent: () => import('./features/personal/personal.component').then(m => m.PersonalComponent)
      },
      {
        path: 'rutas',
        canActivate: [roleGuard],
        data: { roles: ['ADM', 'SUP', 'DRV', 'SEC'] },
        loadComponent: () => import('./features/rutas/rutas.component').then(m => m.RutasComponent)
      },
      {
        path: 'reportes',
        canActivate: [roleGuard],
        data: { roles: ['ADM', 'SUP'] },
        loadComponent: () => import('./features/reportes/reportes.component').then(m => m.ReportesComponent)
      }
    ]
  },
  { path: '**', redirectTo: '/login' }
];
