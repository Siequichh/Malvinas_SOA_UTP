import { Component, OnInit, OnDestroy, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { DispatchService } from '../../core/services/dispatch.service';
import { AuthService } from '../../core/services/auth.service';
import { EmployeeService } from '../../core/services/employee.service';
import { VehicleService } from '../../core/services/vehicle.service';
import { NotificationService } from '../../core/services/notification.service';
import { parseApiError } from '../../core/utils/error.utils';
import { SafeUrl } from '@angular/platform-browser';
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
import { QRCodeComponent } from 'angularx-qrcode';

// En Ruta first, then Programado, then Completado, then Cancelado
const DISPATCH_PRIORITY: Record<string, number> = { '02': 0, '01': 1, '03': 2, '04': 3 };

@Component({
  selector: 'app-rutas',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, TagModule,
    DialogModule, InputTextModule, SelectModule, MultiSelectModule, DatePickerModule,
    ToastModule, TextareaModule, ConfirmDialogModule, IconFieldModule, InputIconModule,
    QRCodeComponent],
  providers: [MessageService, ConfirmationService],
  templateUrl: './rutas.component.html',
  styleUrls: ['./rutas.component.scss']
})
export class RutasComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  readonly userRole = this.authService.userRole;

  dispatches = signal<any[]>([]);
  deliveryPoints = signal<any[]>([]);
  drivers = signal<any[]>([]);
  cargadoVehicles = signal<any[]>([]);
  loading = signal(true);
  dialogVisible = signal(false);
  submitting = signal(false);
  editingId = signal<number | null>(null);
  statusFilter = signal<string | null>(null);
  scanDialogVisible = signal(false);
  scannedOC = signal<string | null>(null);
  scannedResult = signal<{ valid: boolean; message: string } | null>(null);
  qrDownloadUrl = signal<SafeUrl | null>(null);
  form = signal<any>({ vehiclePlate: '', driverId: null, scheduledDepartureTime: '', remarks: '', deliveryPointIds: [] });
  departureTime = signal<Date | null>(null);
  private prevDispatchIds = new Set<number>();
  private prevStatusMap = new Map<number, string>();
  private html5QrCode: any = null;

  readonly filteredDispatches = computed(() => {
    const f = this.statusFilter();
    return f ? this.dispatches().filter((x: any) => x.statusCode === f) : this.dispatches();
  });

  readonly activeDispatch = computed(() =>
    this.dispatches().find((d: any) => d.statusCode === '02' && d.driverId === this.authService.currentUser()?.id)
  );

  readonly statusOptions = [
    { label: 'Programado', value: '01', severity: 'info'    },
    { label: 'En Ruta',    value: '02', severity: 'warning' },
    { label: 'Completado', value: '03', severity: 'success' },
    { label: 'Cancelado',  value: '04', severity: 'danger'  }
  ];

  constructor(
    private dispatchService: DispatchService,
    private employeeService: EmployeeService,
    private vehicleService: VehicleService,
    private activatedRoute: ActivatedRoute,
    private notificationService: NotificationService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit() {
    this.dispatchService.getDeliveryPoints().subscribe(dp => this.deliveryPoints.set(dp));
    const role = this.userRole();
    if (role === 'ADM' || role === 'SUP') {
      this.vehicleService.getVehiclesByStatus('03').subscribe(v => this.cargadoVehicles.set(v));
    }
    if (role === 'ADM' || role === 'SUP' || role === 'SEC') {
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
    // Open create dialog with pre-filled plate when navigated from Cargado notification
    this.activatedRoute.queryParams.subscribe(params => {
      if (params['plate'] && (role === 'ADM' || role === 'SUP')) {
        this.openCreateWithPlate(params['plate']);
      }
    });
  }

  ngOnDestroy() {
    this.notificationService.pauseDrvReminder();
    if (this.html5QrCode) { this.html5QrCode.stop().catch(() => {}); }
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

        // DRV: reminder de despachos pendientes (layout maneja SUP/ADM y MOV globalmente)
        if (role === 'DRV') {
          const pending = enriched.filter((x: any) => x.statusCode === '01');
          this.notificationService.startDrvReminder(pending.map((x: any) => x.id), '');
        } else if (role === 'SEC') {
          if (this.prevDispatchIds.size > 0) {
            for (const d of enriched) {
              const prev = this.prevStatusMap.get(d.id);
              if (prev === '01' && d.statusCode === '02') {
                this.notificationService.notify('pi pi-shield', 'Vehículo en ruta', `${d.vehiclePlate} salió — OC: ${d.loadingOrderCode}`, '/rutas', 'info');
              }
            }
          }
        }
        this.prevDispatchIds = new Set<number>(enriched.map((x: any) => x.id));
        this.prevStatusMap = new Map(enriched.map((x: any) => [x.id, x.statusCode]));
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
    this.vehicleService.getVehiclesByStatus('03').subscribe(v => this.cargadoVehicles.set(v));
    this.editingId.set(null);
    this.form.set({ vehiclePlate: '', driverId: null, scheduledDepartureTime: '', remarks: '', deliveryPointIds: [] });
    this.departureTime.set(null);
    this.dialogVisible.set(true);
  }

  openCreateWithPlate(plate: string) {
    this.vehicleService.getVehiclesByStatus('03').subscribe(v => {
      this.cargadoVehicles.set(v);
      this.editingId.set(null);
      this.form.set({ vehiclePlate: plate, driverId: null, scheduledDepartureTime: '', remarks: '', deliveryPointIds: [] });
      this.departureTime.set(null);
      this.dialogVisible.set(true);
    });
  }

  onQrCodeURL(url: SafeUrl) {
    this.qrDownloadUrl.set(url);
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
    const editId = this.editingId();
    const driverName = !editId
      ? (this.drivers().find((d: any) => d.id === f.driverId)?.fullName ?? 'conductor')
      : '';
    this.submitting.set(true);
    const req = editId
      ? this.dispatchService.updateDispatch(editId, f)
      : this.dispatchService.createDispatch(f);
    req.subscribe({
      next: () => {
        this.submitting.set(false);
        this.dialogVisible.set(false);
        this.editingId.set(null);
        this.loadDispatches();
        if (!editId) {
          this.notificationService.notify('pi pi-calendar-plus', 'Despacho programado',
            `Conductor ${driverName} ha sido asignado — ${f.vehiclePlate}`, '/rutas', 'success');
        }
        this.messageService.add({ severity: 'success', summary: editId ? 'Despacho actualizado' : 'Despacho creado', detail: editId ? 'Cambios guardados correctamente' : 'Despacho programado correctamente' });
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

  confirmAccept(dispatch: any) {
    this.confirmationService.confirm({
      message: `¿Aceptar la salida del vehículo <strong>${dispatch.vehiclePlate}</strong>?<br>
                <small style="color:#94a3b8">Se generará la Orden de Carga y el vehículo quedará EN RUTA.</small>`,
      header: 'Aceptar Despacho',
      icon: 'pi pi-check-circle',
      acceptLabel: 'Aceptar salida',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-warning',
      accept: () => this.acceptDispatch(dispatch)
    });
  }

  private acceptDispatch(dispatch: any) {
    this.submitting.set(true);
    this.dispatchService.acceptDispatch(dispatch.id).subscribe({
      next: (d) => {
        this.submitting.set(false);
        this.loadDispatches();
        this.notificationService.stopDrvReminder();
        this.messageService.add({ severity: 'success', summary: 'Salida aceptada', detail: `Orden de Carga: ${d.loadingOrderCode}` });
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

  openEdit(dispatch: any) {
    this.editingId.set(dispatch.id);
    this.form.set({
      vehiclePlate: dispatch.vehiclePlate,
      driverId: dispatch.driverId,
      scheduledDepartureTime: dispatch.scheduledDepartureTime ?? '',
      remarks: dispatch.remarks ?? '',
      deliveryPointIds: dispatch.points?.map((p: any) => p.deliveryPointId) ?? []
    });
    if (dispatch.scheduledDepartureTime) {
      const [hh, mm] = dispatch.scheduledDepartureTime.split(':');
      const d = new Date(); d.setHours(+hh, +mm);
      this.departureTime.set(d);
    }
    this.dialogVisible.set(true);
  }

  confirmCancelDispatch(dispatch: any) {
    this.confirmationService.confirm({
      message: `¿Cancelar el despacho del vehículo <strong>${dispatch.vehiclePlate}</strong>?`,
      header: 'Cancelar Despacho',
      icon: 'pi pi-times-circle',
      acceptLabel: 'Cancelar despacho',
      rejectLabel: 'Mantener',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.doCancelDispatch(dispatch.id)
    });
  }

  private doCancelDispatch(id: number) {
    this.submitting.set(true);
    this.dispatchService.cancelDispatch(id).subscribe({
      next: () => {
        this.submitting.set(false);
        this.loadDispatches();
        this.messageService.add({ severity: 'warn', summary: 'Cancelado', detail: 'Despacho cancelado. Vehículo disponible.' });
      },
      error: (e) => {
        this.submitting.set(false);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: parseApiError(e) });
      }
    });
  }

  confirmReject(dispatch: any) {
    this.confirmationService.confirm({
      message: `¿Rechazar el despacho del vehículo <strong>${dispatch.vehiclePlate}</strong>?`,
      header: 'Rechazar Despacho',
      icon: 'pi pi-times-circle',
      acceptLabel: 'Rechazar',
      rejectLabel: 'Mantener',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.rejectDispatch(dispatch)
    });
  }

  private rejectDispatch(dispatch: any) {
    this.submitting.set(true);
    this.dispatchService.cancelDispatch(dispatch.id).subscribe({
      next: () => {
        this.submitting.set(false);
        this.notificationService.stopDrvReminder();
        this.loadDispatches();
        this.messageService.add({ severity: 'warn', summary: 'Rechazado', detail: 'Has rechazado el despacho asignado.' });
      },
      error: (e) => {
        this.submitting.set(false);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: parseApiError(e) });
      }
    });
  }

  openScanDialog() {
    this.scannedOC.set(null);
    this.scannedResult.set(null);
    this.scanDialogVisible.set(true);
    setTimeout(() => this.startCamera(), 300);
  }

  private async startCamera() {
    const { Html5Qrcode } = await import('html5-qrcode');
    const onSuccess = (code: string) => {
      this.scannedOC.set(code);
      const match = this.dispatches().find((d: any) => d.loadingOrderCode === code && d.statusCode === '02');
      this.scannedResult.set(match
        ? { valid: true, message: `OC válida — Vehículo ${match.vehiclePlate}` }
        : { valid: false, message: 'OC no encontrada o vehículo no está EN RUTA' });
      this.stopCamera();
    };
    try {
      this.html5QrCode = new Html5Qrcode('qr-reader');
      await this.html5QrCode.start({ facingMode: 'environment' }, { fps: 15, qrbox: { width: 250, height: 250 } }, onSuccess, () => {});
    } catch {
      // facingMode constraint failed — stop partial init and try by camera ID
      try { await this.html5QrCode?.stop(); } catch {}
      this.html5QrCode = null;
      try {
        const cameras = await Html5Qrcode.getCameras();
        if (cameras?.length) {
          this.html5QrCode = new Html5Qrcode('qr-reader');
          await this.html5QrCode.start(cameras[cameras.length - 1].id, { fps: 15, qrbox: { width: 250, height: 250 } }, onSuccess, () => {});
        }
      } catch { /* camera not available */ }
    }
  }

  closeScanDialog() {
    this.stopCamera();
    this.scanDialogVisible.set(false);
  }

  private stopCamera() {
    if (this.html5QrCode) {
      this.html5QrCode.stop().catch(() => {});
      this.html5QrCode = null;
    }
  }

  updateForm(field: string, value: any) {
    this.form.update(f => ({ ...f, [field]: value }));
  }
}
