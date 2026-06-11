import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
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
  private readonly TOKEN_KEY = 'access_token';
  private readonly REFRESH_KEY = 'refresh_token';
  private readonly USER_KEY = 'current_user';

  private _currentUser = signal<LoginResponse['employee'] | null>(this.loadUser());
  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = computed(() => !!this._currentUser());
  readonly userRole = computed(() => this._currentUser()?.role ?? '');

  constructor(private http: HttpClient, private router: Router) {}

  login(dni: string, password: string) {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/api/auth/login`, { dni, password }).pipe(
      tap(response => {
        localStorage.setItem(this.TOKEN_KEY, response.accessToken);
        localStorage.setItem(this.REFRESH_KEY, response.refreshToken);
        localStorage.setItem(this.USER_KEY, JSON.stringify(response.employee));
        this._currentUser.set(response.employee);
      })
    );
  }

  logout() {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_KEY);
    localStorage.removeItem(this.USER_KEY);
    this._currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private loadUser(): LoginResponse['employee'] | null {
    const data = localStorage.getItem(this.USER_KEY);
    return data ? JSON.parse(data) : null;
  }
}
