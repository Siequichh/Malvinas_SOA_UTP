import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !req.url.includes('/auth/')) {
        const refreshToken = authService.getRefreshToken();
        if (refreshToken) {
          return authService.refreshAccessToken().pipe(
            switchMap(response => {
              const retried = req.clone({
                setHeaders: { Authorization: `Bearer ${response.accessToken}` }
              });
              return next(retried);
            }),
            catchError(() => {
              authService.logout();
              return throwError(() => err);
            })
          );
        }
        authService.logout();
      }
      return throwError(() => err);
    })
  );
};
