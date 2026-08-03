import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-login',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.component.html',
  imports: [
    FormsModule
  ]
})
export class LoginComponent {
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly emailInput = signal('');
  protected readonly codeInput = signal('');

  protected readonly emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.emailInput().trim()));
  protected readonly codeValid = computed(() => /^\d{6}$/.test(this.codeInput().trim()));

  onEmailInput(value: string): void {
    this.emailInput.set(value);
  }

  onCodeInput(value: string): void {
    this.codeInput.set(value.replace(/\D/g, '').slice(0, 6));
  }

  submitEmail(): void {
    if (!this.emailValid() || this.auth.loading()) return;
    this.auth.requestOtp(this.emailInput().trim()).subscribe();
  }

  submitCode(): void {
    if (!this.codeValid() || this.auth.loading()) return;
    this.auth.verifyOtp(this.codeInput().trim()).subscribe({
      next: (res) => this.router.navigateByUrl(res.user.isNew ? '/onboarding' : '/'),
    });
  }

  resend(): void {
    this.auth.resendOtp()?.subscribe();
  }

  changeEmail(): void {
    this.codeInput.set('');
    this.auth.backToEmail();
  }
}
