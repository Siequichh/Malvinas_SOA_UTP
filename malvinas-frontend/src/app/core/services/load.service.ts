import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class LoadService {
  private apiUrl = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient) {}

  getLoads() { return this.http.get<any[]>(`${this.apiUrl}/loads`); }
  getActiveLoads() { return this.http.get<any[]>(`${this.apiUrl}/loads/active`); }
  getLoadById(id: number) { return this.http.get<any>(`${this.apiUrl}/loads/${id}`); }
  createLoad(data: any) { return this.http.post<any>(`${this.apiUrl}/loads`, data); }
  completeLoad(id: number) { return this.http.put<any>(`${this.apiUrl}/loads/${id}/complete`, {}); }
  cancelLoad(id: number) { return this.http.put<any>(`${this.apiUrl}/loads/${id}/cancel`, {}); }
}
