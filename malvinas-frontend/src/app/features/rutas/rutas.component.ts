import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DispatchService } from '../../core/services/dispatch.service';
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
  selector: 'app-rutas',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, TagModule,
    DialogModule, InputTextModule, SelectModule, ToastModule, TextareaModule],
  providers: [MessageService],
  templateUrl: './rutas.component.html',
  styleUrls: ['./rutas.component.scss']
})
export class RutasComponent implements OnInit {
  dispatches = signal<any[]>([]);
  deliveryPoints = signal<any[]>([]);
  loading = signal(true);
  dialogVisible = signal(false);
  form = signal<any>({ vehiclePlate: '', driverId: null, scheduledDepartureTime: '', remarks: '', deliveryPointIds: [] });

  readonly statusOptions = [
    { label: 'Programado', value: '01', severity: 'info'    },
    { label: 'En Ruta',    value: '02', severity: 'warning' },
    { label: 'Completado', value: '03', severity: 'success' },
    { label: 'Cancelado',  value: '04', severity: 'danger'  }
  ];

  constructor(private dispatchService: DispatchService, private messageService: MessageService) {}

  ngOnInit() {
    this.loadDispatches();
    this.dispatchService.getDeliveryPoints().subscribe(dp => this.deliveryPoints.set(dp));
  }

  loadDispatches() {
    this.loading.set(true);
    this.dispatchService.getDispatches().subscribe({
      next: (d) => { this.dispatches.set(d); this.loading.set(false); },
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
    this.form.set({ vehiclePlate: '', driverId: null, scheduledDepartureTime: '', remarks: '', deliveryPointIds: [] });
    this.dialogVisible.set(true);
  }

  saveDispatch() {
    this.dispatchService.createDispatch(this.form()).subscribe({
      next: () => {
        this.dialogVisible.set(false);
        this.loadDispatches();
        this.messageService.add({ severity: 'success', summary: 'Despacho creado', detail: 'Despacho programado correctamente' });
      },
      error: (e) => this.messageService.add({ severity: 'error', summary: 'Error', detail: e.error?.message || 'Error al crear despacho' })
    });
  }

  registerDeparture(dispatch: any) {
    this.dispatchService.registerDeparture(dispatch.id).subscribe({
      next: (d) => {
        this.loadDispatches();
        this.messageService.add({ severity: 'success', summary: 'Salida registrada', detail: `Orden de Carga: ${d.loadingOrderCode}` });
      },
      error: (e) => this.messageService.add({ severity: 'error', summary: 'Error', detail: e.error?.message || 'Error al registrar salida' })
    });
  }

  completeDispatch(dispatch: any) {
    this.dispatchService.completeDispatch(dispatch.id).subscribe({
      next: () => { this.loadDispatches(); this.messageService.add({ severity: 'success', summary: 'Ruta cerrada', detail: 'Vehiculo retornado a base' }); },
      error: (e) => this.messageService.add({ severity: 'error', summary: 'Error', detail: e.error?.message || 'Error al cerrar ruta' })
    });
  }

  updateForm(field: string, value: any) {
    this.form.update(f => ({ ...f, [field]: value }));
  }
}
