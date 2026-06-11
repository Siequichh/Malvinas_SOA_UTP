import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

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
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'vehiculos',
        loadComponent: () => import('./features/vehiculos/vehiculos.component').then(m => m.VehiculosComponent)
      },
      {
        path: 'cargas',
        loadComponent: () => import('./features/cargas/cargas.component').then(m => m.CargasComponent)
      },
      {
        path: 'personal',
        loadComponent: () => import('./features/personal/personal.component').then(m => m.PersonalComponent)
      },
      {
        path: 'rutas',
        loadComponent: () => import('./features/rutas/rutas.component').then(m => m.RutasComponent)
      },
      {
        path: 'reportes',
        loadComponent: () => import('./features/reportes/reportes.component').then(m => m.ReportesComponent)
      }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
