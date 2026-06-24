import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EmployeeService } from '../../core/services/employee.service';
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
  selector: 'app-personal',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, TagModule,
    DialogModule, InputTextModule, SelectModule, ToastModule, IconFieldModule, InputIconModule],
  providers: [MessageService],
  templateUrl: './personal.component.html',
  styleUrls: ['./personal.component.scss']
})
export class PersonalComponent implements OnInit {
  readonly userRole = inject(AuthService).userRole;
  employees = signal<any[]>([]);
  roles = signal<any[]>([]);
  loading = signal(true);
  dialogVisible = signal(false);
  submitting = signal(false);
  editMode = signal(false);
  selectedEmployee = signal<any>(null);
  form = signal<any>({ dni: '', firstName: '', lastName: '', email: '', phone: '', roleId: null, password: '' });

  constructor(private employeeService: EmployeeService, private messageService: MessageService) {}

  ngOnInit() {
    this.loadEmployees();
    this.employeeService.getRoles().subscribe(r => this.roles.set(r));
  }

  loadEmployees() {
    this.loading.set(true);
    this.employeeService.getEmployees().subscribe({
      next: (e) => { this.employees.set(e); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  openCreate() {
    this.form.set({ dni: '', firstName: '', lastName: '', email: '', phone: '', roleId: null, password: '' });
    this.editMode.set(false);
    this.dialogVisible.set(true);
  }

  openEdit(emp: any) {
    this.selectedEmployee.set(emp);
    this.form.set({ ...emp });
    this.editMode.set(true);
    this.dialogVisible.set(true);
  }

  saveEmployee() {
    const f = this.form();
    if (!f.dni || !f.firstName || !f.lastName || !f.roleId) {
      this.messageService.add({ severity: 'warn', summary: 'Campos requeridos', detail: 'Completa DNI, nombre, apellido y rol.' });
      return;
    }
    this.submitting.set(true);
    const obs = this.editMode()
      ? this.employeeService.updateEmployee(this.selectedEmployee().id, f)
      : this.employeeService.createEmployee(f);
    obs.subscribe({
      next: () => {
        this.submitting.set(false);
        this.dialogVisible.set(false);
        this.loadEmployees();
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Empleado guardado' });
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
