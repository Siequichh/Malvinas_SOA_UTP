import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LoadService } from '../../core/services/load.service';
import { VehicleService } from '../../core/services/vehicle.service';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { ToastModule } from 'primeng/toast';
import { TextareaModule } from 'primeng/textarea';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-cargas',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, TagModule,
    DialogModule, InputTextModule, SelectModule, ToastModule, TextareaModule],
  providers: [MessageService],
  templateUrl: './cargas.component.html',
  styleUrls: ['./cargas.component.scss']
})
export class CargasComponent implements OnInit {
  loads = signal<any[]>([]);
  availableVehicles = signal<any[]>([]);
  loading = signal(true);
  dialogVisible = signal(false);
  form = signal<any>({ vehiclePlate: '', mobilizerId: null, loadingPlant: 'Babel - Huachipa', remarks: '' });

  readonly statusOptions = [
    { label: 'Pendiente', value: '01', severity: 'secondary' },
    { label: 'En Proceso', value: '02', severity: 'warning' },
    { label: 'Completado', value: '03', severity: 'success' },
    { label: 'Cancelado',  value: '04', severity: 'danger'  }
  ];

  constructor(private loadService: LoadService, private vehicleService: VehicleService,
              private messageService: MessageService) {}

  ngOnInit() {
    this.loadLoads();
    this.vehicleService.getVehiclesByStatus('01').subscribe(v => this.availableVehicles.set(v));
  }

  loadLoads() {
    this.loading.set(true);
    this.loadService.getLoads().subscribe({
      next: (l) => { this.loads.set(l); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  getStatusSeverity(code: string): string {
    return this.statusOptions.find(s => s.value === code)?.severity ?? 'secondary';
  }

  getStatusLabel(code: string): string {
    return this.statusOptions.find(s => s.value === code)?.label ?? code;
  }

  openCreate() {
    this.form.set({ vehiclePlate: '', mobilizerId: null, loadingPlant: 'Babel - Huachipa', remarks: '' });
    this.dialogVisible.set(true);
  }

  saveLoad() {
    this.loadService.createLoad(this.form()).subscribe({
      next: () => {
        this.dialogVisible.set(false);
        this.loadLoads();
        this.messageService.add({ severity: 'success', summary: 'Carga iniciada', detail: 'Proceso de carga registrado' });
      },
      error: (e) => this.messageService.add({ severity: 'error', summary: 'Error', detail: e.error?.message || 'Error al iniciar carga' })
    });
  }

  completeLoad(load: any) {
    this.loadService.completeLoad(load.id).subscribe({
      next: () => { this.loadLoads(); this.messageService.add({ severity: 'success', summary: 'Completado', detail: 'Carga completada. Vehiculo listo para despacho.' }); },
      error: (e) => this.messageService.add({ severity: 'error', summary: 'Error', detail: e.error?.message || 'Error al completar' })
    });
  }

  cancelLoad(load: any) {
    this.loadService.cancelLoad(load.id).subscribe({
      next: () => { this.loadLoads(); this.messageService.add({ severity: 'warn', summary: 'Cancelado', detail: 'Carga cancelada. Vehiculo disponible.' }); },
      error: (e) => this.messageService.add({ severity: 'error', summary: 'Error', detail: e.error?.message || 'Error al cancelar' })
    });
  }

  updateForm(field: string, value: any) {
    this.form.update(f => ({ ...f, [field]: value }));
  }
}
