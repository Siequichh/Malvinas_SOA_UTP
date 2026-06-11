import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { MessageModule } from 'primeng/message';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, ButtonModule, InputTextModule, PasswordModule,
            MessageModule, IconFieldModule, InputIconModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  dni = signal('');
  password = signal('');
  loading = signal(false);
  error = signal('');

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit() {
    if (!this.dni() || !this.password()) return;
    this.loading.set(true);
    this.error.set('');
    this.authService.login(this.dni(), this.password()).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.error.set(err.error?.message || 'Credenciales invalidas');
        this.loading.set(false);
      }
    });
  }
}
