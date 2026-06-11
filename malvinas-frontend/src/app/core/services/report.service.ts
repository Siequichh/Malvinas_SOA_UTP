import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private apiUrl = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient) {}

  getDashboard() { return this.http.get<any>(`${this.apiUrl}/reports/dashboard`); }
  getFleetReport() { return this.http.get<any>(`${this.apiUrl}/reports/fleet`); }
  getKpis() { return this.http.get<any[]>(`${this.apiUrl}/kpis`); }
}
