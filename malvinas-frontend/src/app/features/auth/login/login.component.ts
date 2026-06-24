import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { ROLE_HOME } from '../../../core/guards/auth.guard';
import { parseLoginError } from '../../../core/utils/error.utils';
import { saveCredentials, loadCredentials, clearCredentials } from '../../../core/utils/credential-store';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { CheckboxModule } from 'primeng/checkbox';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, ButtonModule, InputTextModule,
            IconFieldModule, InputIconModule, CheckboxModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  dni          = signal('');
  password     = signal('');
  showPassword = signal(false);
  rememberMe   = signal(false);
  loading      = signal(false);
  error        = signal('');

  constructor(private authService: AuthService, private router: Router) {}

  async ngOnInit() {
    const creds = await loadCredentials();
    if (creds) {
      this.dni.set(creds.dni);
      this.password.set(creds.password);
      this.rememberMe.set(true);
    }
  }

  togglePassword() { this.showPassword.update(v => !v); }

  async onSubmit() {
    if (!this.dni() || !this.password()) return;
    this.loading.set(true);
    this.error.set('');
    if (this.rememberMe()) {
      await saveCredentials(this.dni(), this.password());
    } else {
      clearCredentials();
    }
    this.authService.login(this.dni(), this.password(), this.rememberMe()).subscribe({
      next: () => {
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
