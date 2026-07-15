import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LoadService } from '../../core/services/load.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { parseApiError } from '../../core/utils/error.utils';
import { VehicleService } from '../../core/services/vehicle.service';
import { EmployeeService } from '../../core/services/employee.service';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { ToastModule } from 'primeng/toast';
import { TextareaModule } from 'primeng/textarea';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { MessageService, ConfirmationService } from 'primeng/api';

const LOAD_PRIORITY_MOV: Record<string, number> = { '02': 0, '01': 1, '03': 2, '04': 3 };
const LOAD_PRIORITY_SUP: Record<string, number> = { '03': 0, '02': 1, '01': 2, '04': 3 };

@Component({
  selector: 'app-cargas',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, TagModule,
    DialogModule, InputTextModule, SelectModule, ToastModule, TextareaModule,
    ConfirmDialogModule, IconFieldModule, InputIconModule],
  providers: [MessageService, ConfirmationService],
  templateUrl: './cargas.component.html',
  styleUrls: ['./cargas.component.scss']
})
export class CargasComponent implements OnInit {
  private authService = inject(AuthService);
  readonly userRole = this.authService.userRole;

  private prevLoadStatusMap = new Map<number, string>();
  loads = signal<any[]>([]);
  availableVehicles = signal<any[]>([]);
  mobilizers = signal<any[]>([]);
  loading = signal(true);
  dialogVisible = signal(false);
  submitting = signal(false);
  statusFilter = signal<string | null>(null);
  editingId = signal<number | null>(null);
  form = signal<any>({ vehiclePlate: '', mobilizerId: null, loadingPlant: 'Babel - Huachipa', remarks: '' });

  readonly filteredLoads = computed(() => {
    const f = this.statusFilter();
    return f ? this.loads().filter((x: any) => x.statusCode === f) : this.loads();
  });

  readonly statusOptions = [
    { label: 'Pendiente', value: '01', severity: 'secondary' },
    { label: 'En Proceso', value: '02', severity: 'warning' },
    { label: 'Completado', value: '03', severity: 'success' },
    { label: 'Cancelado',  value: '04', severity: 'danger'  }
  ];

  constructor(
    private loadService: LoadService,
    private vehicleService: VehicleService,
    private employeeService: EmployeeService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.loadLoads();
    this.vehicleService.getVehiclesByStatus('01').subscribe(v => this.availableVehicles.set(v));
    this.employeeService.getEmployees().subscribe(emps =>
      this.mobilizers.set(emps.filter((e: any) => e.roleCode === 'MOV'))
    );
  }

  loadLoads() {
    this.loading.set(true);
    this.loadService.getLoads().subscribe({
      next: (l) => {
        const priority = this.userRole() === 'MOV' ? LOAD_PRIORITY_MOV : LOAD_PRIORITY_SUP;
        const sorted = [...l].sort((a, b) => {
          const diff = (priority[a.statusCode] ?? 9) - (priority[b.statusCode] ?? 9);
          return diff !== 0 ? diff : b.id - a.id;
        });
        // Notify SUP/ADM when a load changes to Completado/Cargado
        const role = this.userRole();
        if ((role === 'SUP' || role === 'ADM') && this.prevLoadStatusMap.size > 0) {
          for (const load of sorted) {
            const prev = this.prevLoadStatusMap.get(load.id);
            if (prev && prev !== '03' && load.statusCode === '03') {
              this.notificationService.notify(
                'pi pi-check-circle', 'Vehículo Cargado',
                `${load.vehiclePlate} está listo para despacho — click para programar`,
                `/rutas?plate=${load.vehiclePlate}`, 'success'
              );
            }
          }
        }
        this.prevLoadStatusMap = new Map(sorted.map((l: any) => [l.id, l.statusCode]));
        this.loads.set(sorted);
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
    const user = this.authService.currentUser();
    this.editingId.set(null);
    this.form.set({ vehiclePlate: '', mobilizerId: user?.id ?? null, loadingPlant: 'Babel - Huachipa', remarks: '' });
    this.dialogVisible.set(true);
  }

  openEdit(load: any) {
    if (load.statusCode !== '02') {
      this.messageService.add({ severity: 'warn', summary: 'No se puede editar', detail: 'Solo puedes editar cargas en proceso.' });
      return;
    }
    this.editingId.set(load.id);
    this.form.set({ vehiclePlate: load.vehiclePlate, mobilizerId: load.mobilizerId, loadingPlant: load.loadingPlant, remarks: load.remarks });
    this.dialogVisible.set(true);
  }

  saveLoad() {
    const f = this.form();
    const id = this.editingId();

    if (id) {
      // Actualizar
      if (!f.loadingPlant && !f.remarks) {
        this.messageService.add({ severity: 'warn', summary: 'Sin cambios', detail: 'Realiza al menos un cambio.' });
        return;
      }
      this.submitting.set(true);
      this.loadService.updateLoad(id, f).subscribe({
        next: () => {
          this.submitting.set(false);
          this.dialogVisible.set(false);
          this.loadLoads();
          this.messageService.add({ severity: 'success', summary: 'Actualizado', detail: 'Carga actualizada.' });
        },
        error: (e) => {
          this.submitting.set(false);
          this.messageService.add({ severity: 'error', summary: 'Error', detail: parseApiError(e) });
        }
      });
    } else {
      // Crear
      if (!f.vehiclePlate || !f.mobilizerId) {
        this.messageService.add({ severity: 'warn', summary: 'Campos requeridos', detail: 'Selecciona vehículo y movilizador.' });
        return;
      }
      this.submitting.set(true);
      this.loadService.createLoad(f).subscribe({
        next: () => {
          this.submitting.set(false);
          this.dialogVisible.set(false);
          this.loadLoads();
          this.messageService.add({ severity: 'success', summary: 'Carga iniciada', detail: 'Proceso de carga registrado' });
        },
        error: (e) => {
          this.submitting.set(false);
          this.messageService.add({ severity: 'error', summary: 'Error', detail: parseApiError(e) });
        }
      });
    }
  }

  confirmComplete(load: any) {
    this.confirmationService.confirm({
      message: `¿Completar la carga del vehículo <strong>${load.vehiclePlate}</strong>?<br>
                <small style="color:#94a3b8">El vehículo quedará listo para despacho.</small>`,
      header: 'Completar Carga',
      icon: 'pi pi-check-circle',
      acceptLabel: 'Completar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-success',
      accept: () => this.doCompleteLoad(load)
    });
  }

  confirmCancel(load: any) {
    this.confirmationService.confirm({
      message: `¿Cancelar la carga del vehículo <strong>${load.vehiclePlate}</strong>?<br>
                <small style="color:#ef4444">Esta acción no se puede deshacer.</small>`,
      header: 'Cancelar Carga',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Cancelar carga',
      rejectLabel: 'Mantener',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.doCancelLoad(load)
    });
  }

  private doCompleteLoad(load: any) {
    this.submitting.set(true);
    this.loadService.completeLoad(load.id).subscribe({
      next: () => {
        this.submitting.set(false);
        this.loadLoads();
        this.messageService.add({ severity: 'success', summary: 'Completado', detail: 'Carga completada. Vehículo listo para despacho.' });
      },
      error: (e) => {
        this.submitting.set(false);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: parseApiError(e) });
      }
    });
  }

  private doCancelLoad(load: any) {
    this.submitting.set(true);
    this.loadService.cancelLoad(load.id).subscribe({
      next: () => {
        this.submitting.set(false);
        this.loadLoads();
        this.messageService.add({ severity: 'warn', summary: 'Cancelado', detail: 'Carga cancelada. Vehículo disponible.' });
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
