import { Component, OnInit, OnDestroy, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DispatchService } from '../../core/services/dispatch.service';
import { AuthService } from '../../core/services/auth.service';
import { EmployeeService } from '../../core/services/employee.service';
import { parseApiError } from '../../core/utils/error.utils';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { MultiSelectModule } from 'primeng/multiselect';
import { DatePickerModule } from 'primeng/datepicker';
import { ToastModule } from 'primeng/toast';
import { TextareaModule } from 'primeng/textarea';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { MessageService, ConfirmationService } from 'primeng/api';

// En Ruta first, then Programado, then Completado, then Cancelado
const DISPATCH_PRIORITY: Record<string, number> = { '02': 0, '01': 1, '03': 2, '04': 3 };

@Component({
  selector: 'app-rutas',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, TagModule,
    DialogModule, InputTextModule, SelectModule, MultiSelectModule, DatePickerModule,
    ToastModule, TextareaModule, ConfirmDialogModule, IconFieldModule, InputIconModule],
  providers: [MessageService, ConfirmationService],
  templateUrl: './rutas.component.html',
  styleUrls: ['./rutas.component.scss']
})
export class RutasComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  readonly userRole = this.authService.userRole;

  private refreshTimer: ReturnType<typeof setInterval> | null = null;

  dispatches = signal<any[]>([]);
  deliveryPoints = signal<any[]>([]);
  drivers = signal<any[]>([]);
  loading = signal(true);
  dialogVisible = signal(false);
  submitting = signal(false);
  form = signal<any>({ vehiclePlate: '', driverId: null, scheduledDepartureTime: '', remarks: '', deliveryPointIds: [] });
  departureTime = signal<Date | null>(null);

  readonly statusOptions = [
    { label: 'Programado', value: '01', severity: 'info'    },
    { label: 'En Ruta',    value: '02', severity: 'warning' },
    { label: 'Completado', value: '03', severity: 'success' },
    { label: 'Cancelado',  value: '04', severity: 'danger'  }
  ];

  constructor(
    private dispatchService: DispatchService,
    private employeeService: EmployeeService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit() {
    this.dispatchService.getDeliveryPoints().subscribe(dp => this.deliveryPoints.set(dp));
    const role = this.userRole();
    if (role === 'ADM' || role === 'SUP') {
      this.employeeService.getEmployees().subscribe({
        next: (emps) => {
          this.drivers.set(emps.filter((e: any) => e.roleCode === 'DRV'));
          this.loadDispatches();
        },
        error: () => this.loadDispatches()
      });
    } else {
      this.loadDispatches();
    }
    this.refreshTimer = setInterval(() => this.loadDispatches(), 30_000);
  }

  ngOnDestroy() {
    if (this.refreshTimer) clearInterval(this.refreshTimer);
  }

  loadDispatches() {
    this.loading.set(true);
    this.dispatchService.getDispatches().subscribe({
      next: (d) => {
        const role = this.userRole();
        const userId = this.authService.currentUser()?.id;

        const filtered = role === 'DRV' ? d.filter((x: any) => x.driverId === userId) : d;

        const driverMap = new Map(this.drivers().map((e: any) => [e.id, `${e.firstName} ${e.lastName}`]));
        const enriched = filtered.map((x: any) => ({
          ...x,
          driverName: driverMap.get(x.driverId) ?? '—'
        }));

        // Sort: En Ruta → Programado → Completado → Cancelado, then newest first
        enriched.sort((a: any, b: any) => {
          const diff = (DISPATCH_PRIORITY[a.statusCode] ?? 9) - (DISPATCH_PRIORITY[b.statusCode] ?? 9);
          return diff !== 0 ? diff : (b.id - a.id);
        });

        this.dispatches.set(enriched);
        this.loading.set(false);
      },
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
    this.departureTime.set(null);
    this.dialogVisible.set(true);
  }

  onTimeChange(date: Date) {
    this.departureTime.set(date);
    if (date) {
      const hh = String(date.getHours()).padStart(2, '0');
      const mm = String(date.getMinutes()).padStart(2, '0');
      this.updateForm('scheduledDepartureTime', `${hh}:${mm}`);
    }
  }

  confirmDeparture(dispatch: any) {
    this.confirmationService.confirm({
      message: `¿Registrar salida del vehículo <strong>${dispatch.vehiclePlate}</strong>?<br>
                <small style="color:#94a3b8">Conductor: ${dispatch.driverName}</small>`,
      header: 'Confirmar Salida',
      icon: 'pi pi-send',
      acceptLabel: 'Registrar salida',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-warning',
      accept: () => this.registerDeparture(dispatch)
    });
  }

  confirmComplete(dispatch: any) {
    this.confirmationService.confirm({
      message: `¿Cerrar ruta del vehículo <strong>${dispatch.vehiclePlate}</strong>?<br>
                <small style="color:#94a3b8">Orden: ${dispatch.loadingOrderCode}</small>`,
      header: 'Cerrar Ruta',
      icon: 'pi pi-home',
      acceptLabel: 'Cerrar ruta',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-success',
      accept: () => this.completeDispatch(dispatch)
    });
  }

  saveDispatch() {
    const f = this.form();
    if (!f.vehiclePlate || !f.driverId) {
      this.messageService.add({ severity: 'warn', summary: 'Campos requeridos', detail: 'Ingresa la placa del vehículo y selecciona un conductor.' });
      return;
    }
    this.submitting.set(true);
    this.dispatchService.createDispatch(f).subscribe({
      next: () => {
        this.submitting.set(false);
        this.dialogVisible.set(false);
        this.loadDispatches();
        this.messageService.add({ severity: 'success', summary: 'Despacho creado', detail: 'Despacho programado correctamente' });
      },
      error: (e) => {
        this.submitting.set(false);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: parseApiError(e) });
      }
    });
  }

  private registerDeparture(dispatch: any) {
    this.submitting.set(true);
    this.dispatchService.registerDeparture(dispatch.id).subscribe({
      next: (d) => {
        this.submitting.set(false);
        this.loadDispatches();
        this.messageService.add({ severity: 'success', summary: 'Salida registrada', detail: `Orden de Carga: ${d.loadingOrderCode}` });
      },
      error: (e) => {
        this.submitting.set(false);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: parseApiError(e) });
      }
    });
  }

  private completeDispatch(dispatch: any) {
    this.submitting.set(true);
    this.dispatchService.completeDispatch(dispatch.id).subscribe({
      next: () => {
        this.submitting.set(false);
        this.loadDispatches();
        this.messageService.add({ severity: 'success', summary: 'Ruta cerrada', detail: 'Vehículo retornado a base' });
      },
      error: (e) => {
        this.submitting.set(false);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: parseApiError(e) });
      }
    });
  }

  updateForm(field: string, value: any) {
    this.form.update(f => ({ ...f, [field]: value }));
  }
}
