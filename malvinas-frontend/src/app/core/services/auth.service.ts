import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  employee: {
    id: number;
    dni: string;
    fullName: string;
    role: string;
  };
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY   = 'access_token';
  private readonly REFRESH_KEY = 'refresh_token';
  private readonly USER_KEY    = 'current_user';

  private _currentUser = signal<LoginResponse['employee'] | null>(this.loadUser());
  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = computed(() => !!this._currentUser());
  readonly userRole = computed(() => this._currentUser()?.role ?? '');

  constructor(private http: HttpClient, private router: Router) {}

  login(dni: string, password: string, remember: boolean) {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/api/auth/login`, { dni, password }).pipe(
      tap(response => {
        const store = remember ? localStorage : sessionStorage;
        store.setItem(this.TOKEN_KEY,   response.accessToken);
        store.setItem(this.REFRESH_KEY, response.refreshToken);
        store.setItem(this.USER_KEY,    JSON.stringify(response.employee));
        this._currentUser.set(response.employee);
      })
    );
  }

  logout() {
    [localStorage, sessionStorage].forEach(s => {
      s.removeItem(this.TOKEN_KEY);
      s.removeItem(this.REFRESH_KEY);
      s.removeItem(this.USER_KEY);
    });
    this._currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY) ?? sessionStorage.getItem(this.TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_KEY) ?? sessionStorage.getItem(this.REFRESH_KEY);
  }

  refreshAccessToken(): Observable<LoginResponse> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<LoginResponse>(`${environment.apiUrl}/api/auth/refresh`, { refreshToken }).pipe(
      tap(response => {
        const store = localStorage.getItem(this.TOKEN_KEY) ? localStorage : sessionStorage;
        store.setItem(this.TOKEN_KEY, response.accessToken);
        if (response.refreshToken) store.setItem(this.REFRESH_KEY, response.refreshToken);
      })
    );
  }

  private loadUser(): LoginResponse['employee'] | null {
    const data = localStorage.getItem(this.USER_KEY) ?? sessionStorage.getItem(this.USER_KEY);
    return data ? JSON.parse(data) : null;
  }
}
