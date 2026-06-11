import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class VehicleService {
  private apiUrl = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient) {}

  getVehicles() { return this.http.get<any[]>(`${this.apiUrl}/vehicles`); }
  getVehicleByPlate(plate: string) { return this.http.get<any>(`${this.apiUrl}/vehicles/${plate}`); }
  getVehiclesByStatus(status: string) { return this.http.get<any[]>(`${this.apiUrl}/vehicles/status/${status}`); }
  createVehicle(data: any) { return this.http.post<any>(`${this.apiUrl}/vehicles`, data); }
  updateVehicle(plate: string, data: any) { return this.http.put<any>(`${this.apiUrl}/vehicles/${plate}`, data); }
  changeStatus(plate: string, data: any) { return this.http.put<any>(`${this.apiUrl}/vehicles/${plate}/status`, data); }
  getVehicleTypes() { return this.http.get<any[]>(`${this.apiUrl}/vehicle-types`); }
}
