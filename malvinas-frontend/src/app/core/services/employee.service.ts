import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private apiUrl = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient) {}

  getEmployees() { return this.http.get<any[]>(`${this.apiUrl}/employees`); }
  getEmployee(id: number) { return this.http.get<any>(`${this.apiUrl}/employees/${id}`); }
  createEmployee(data: any) { return this.http.post<any>(`${this.apiUrl}/employees`, data); }
  updateEmployee(id: number, data: any) { return this.http.put<any>(`${this.apiUrl}/employees/${id}`, data); }
  deactivateEmployee(id: number) { return this.http.delete(`${this.apiUrl}/employees/${id}`); }
  getRoles() { return this.http.get<any[]>(`${this.apiUrl}/roles`); }
}
