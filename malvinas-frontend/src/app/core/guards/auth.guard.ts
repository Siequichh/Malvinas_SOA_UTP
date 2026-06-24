import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  if (authService.isAuthenticated()) return true;
  return router.createUrlTree(['/login']);
};

// Default landing page per role
export const ROLE_HOME: Record<string, string> = {
  ADM: '/dashboard',
  SUP: '/dashboard',
  MOV: '/cargas',
  DRV: '/rutas',
  SEC: '/rutas'
};

// Redirects unauthorized roles to their home page
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const role = inject(AuthService).userRole();
  const allowed = route.data?.['roles'] as string[] | undefined;
  if (!allowed || allowed.includes(role)) return true;
  return inject(Router).createUrlTree([ROLE_HOME[role] ?? '/dashboard']);
};

// Used on the root '' path — always redirects to role's home
export const homeGuard: CanActivateFn = () => {
  const role = inject(AuthService).userRole();
  return inject(Router).createUrlTree([ROLE_HOME[role] ?? '/dashboard']);
};
