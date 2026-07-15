import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VehicleService } from '../../core/services/vehicle.service';
import { AuthService } from '../../core/services/auth.service';
import { parseApiError } from '../../core/utils/error.utils';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { ToastModule } from 'primeng/toast';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-vehiculos',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, TagModule,
    DialogModule, InputTextModule, SelectModule, ToastModule, IconFieldModule, InputIconModule],
  providers: [MessageService],
  templateUrl: './vehiculos.component.html',
  styleUrls: ['./vehiculos.component.scss']
})
export class VehiculosComponent implements OnInit {
  readonly userRole = inject(AuthService).userRole;
  vehicles = signal<any[]>([]);
  vehicleTypes = signal<any[]>([]);
  loading = signal(true);
  dialogVisible = signal(false);
  statusDialogVisible = signal(false);
  submitting = signal(false);
  editMode = signal(false);
  selectedVehicle = signal<any>(null);

  form = signal<any>({ licensePlate: '', vehicleTypeId: null, brand: '', model: '', year: null, color: '' });
  statusForm = signal<any>({ newStatusCode: '', reason: '' });

  statusFilter = signal<string | null>(null);
  readonly filteredVehicles = computed(() => {
    const f = this.statusFilter();
    return f ? this.vehicles().filter((v: any) => v.statusCode === f) : this.vehicles();
  });

  readonly statusOptions = [
    { label: 'Disponible',   value: '01' },
    { label: 'En Carga',     value: '02' },
    { label: 'Cargado',      value: '03' },
    { label: 'En Ruta',      value: '04' },
    { label: 'Mantenimiento',value: '05' }
  ];

  constructor(private vehicleService: VehicleService, private messageService: MessageService) {}

  ngOnInit() {
    this.loadVehicles();
    this.vehicleService.getVehicleTypes().subscribe(t => this.vehicleTypes.set(t));
  }

  loadVehicles() {
    this.loading.set(true);
    this.vehicleService.getVehicles().subscribe({
      next: (v) => {
        const sorted = [...v].sort((a: any, b: any) => {
          const ta = a.modifiedAt ? new Date(a.modifiedAt).getTime() : 0;
          const tb = b.modifiedAt ? new Date(b.modifiedAt).getTime() : 0;
          return tb - ta;
        });
        this.vehicles.set(sorted);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  getStatusSeverity(code: string): string {
    const map: Record<string, string> = { '01': 'success', '02': 'warning', '03': 'info', '04': 'primary', '05': 'danger' };
    return map[code] || 'secondary';
  }

  openCreate() {
    this.form.set({ licensePlate: '', vehicleTypeId: null, brand: '', model: '', year: null, color: '' });
    this.editMode.set(false);
    this.dialogVisible.set(true);
  }

  openEdit(vehicle: any) {
    this.selectedVehicle.set(vehicle);
    // ponytail: VehicleResponse has vehicleTypeName but not vehicleTypeId — resolve by name
    const typeId = this.vehicleTypes().find((t: any) => t.name === vehicle.vehicleTypeName)?.id ?? null;
    this.form.set({ ...vehicle, vehicleTypeId: typeId });
    this.editMode.set(true);
    this.dialogVisible.set(true);
  }

  openStatusChange(vehicle: any) {
    this.selectedVehicle.set(vehicle);
    this.statusForm.set({ newStatusCode: '', reason: '' });
    this.statusDialogVisible.set(true);
  }

  saveVehicle() {
    const f = this.form();
    if (!f.licensePlate || !f.vehicleTypeId) {
      this.messageService.add({ severity: 'warn', summary: 'Campos requeridos', detail: 'Ingresa la placa y selecciona el tipo de vehículo.' });
      return;
    }
    this.submitting.set(true);
    const obs = this.editMode()
      ? this.vehicleService.updateVehicle(this.selectedVehicle().licensePlate, f)
      : this.vehicleService.createVehicle(f);
    obs.subscribe({
      next: () => {
        this.submitting.set(false);
        this.dialogVisible.set(false);
        this.loadVehicles();
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Vehículo guardado' });
      },
      error: (e) => {
        this.submitting.set(false);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: parseApiError(e) });
      }
    });
  }

  changeStatus() {
    if (!this.statusForm().newStatusCode) {
      this.messageService.add({ severity: 'warn', summary: 'Campos requeridos', detail: 'Selecciona el nuevo estado.' });
      return;
    }
    this.submitting.set(true);
    this.vehicleService.changeStatus(this.selectedVehicle().licensePlate, this.statusForm()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.statusDialogVisible.set(false);
        this.loadVehicles();
        this.messageService.add({ severity: 'success', summary: 'Estado actualizado', detail: '' });
      },
      error: (e) => {
        this.submitting.set(false);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: parseApiError(e) });
      }
    });
  }

  updateStatusForm(field: string, value: any) {
    this.statusForm.update(f => ({ ...f, [field]: value }));
  }

  updateForm(field: string, value: any) {
    this.form.update(f => ({ ...f, [field]: value }));
  }
}
