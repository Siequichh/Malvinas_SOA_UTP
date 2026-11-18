import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ROLE_HOME } from '../../../core/guards/auth.guard';
import { parseLoginError } from '../../../core/utils/error.utils';

// "Recordarme" guarda SOLO el DNI. La version anterior guardaba la contraseña cifrada con una
// clave fija escrita en el JS (recuperable por cualquiera con acceso al navegador): se purga.
const LEGACY_CREDS_KEY = 'malvinas_saved_creds';
const SAVED_DNI_KEY = 'malvinas_saved_dni';

function storage(fn: (s: Storage) => string | null | void): string | null {
  try { return (fn(localStorage) as string | null) ?? null; } catch { return null; }
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit, OnDestroy {
  dni          = signal('');
  password     = signal('');
  showPassword = signal(false);
  rememberMe   = signal(false);
  loading      = signal(false);
  error        = signal('');

  protected now = signal(new Date());
  private timer?: ReturnType<typeof setInterval>;

  clock = computed(() =>
    this.now().toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', hour12: false, timeZone: 'America/Lima' }));
  date = computed(() =>
    this.now().toLocaleDateString('es-PE', { weekday: 'short', day: '2-digit', month: 'short', timeZone: 'America/Lima' }));
  shift = computed(() => {
    const h = Number(this.now().toLocaleString('en-US', { hour: 'numeric', hour12: false, timeZone: 'America/Lima' }));
    return h >= 6 && h < 14 ? 'Turno mañana' : h >= 14 && h < 22 ? 'Turno tarde' : 'Turno noche';
  });
  dniValid = computed(() => /^\d{8}$/.test(this.dni()));

  constructor(private authService: AuthService, private notificationService: NotificationService, private router: Router) {}

  ngOnInit() {
    storage(s => s.removeItem(LEGACY_CREDS_KEY));
    const saved = storage(s => s.getItem(SAVED_DNI_KEY));
    if (saved) { this.dni.set(saved); this.rememberMe.set(true); }
    this.timer = setInterval(() => this.now.set(new Date()), 30_000);
  }

  ngOnDestroy() { clearInterval(this.timer); }

  onDni(value: string) { this.dni.set(value.replace(/\D/g, '').slice(0, 8)); }

  togglePassword() { this.showPassword.update(v => !v); }

  onSubmit() {
    if (!this.dniValid()) { this.error.set('El DNI debe tener 8 dígitos.'); return; }
    if (!this.password()) { this.error.set('Ingresa tu contraseña.'); return; }
    this.loading.set(true);
    this.error.set('');
    storage(s => this.rememberMe() ? s.setItem(SAVED_DNI_KEY, this.dni()) : s.removeItem(SAVED_DNI_KEY));

    this.authService.login(this.dni(), this.password(), this.rememberMe()).subscribe({
      next: async () => {
        await this.notificationService.requestPermission();
        const role = this.authService.userRole();
        this.router.navigate([ROLE_HOME[role] ?? '/dashboard']);
      },
      error: (err) => {
        this.error.set(parseLoginError(err));
        this.loading.set(false);
      }
    });
  }
}
