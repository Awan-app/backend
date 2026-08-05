import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { FirebaseMessagingService } from '../../core/services/firebase-messaging.service';

@Component({
  selector: 'app-home',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './home.component.html',
})
export class HomeComponent {
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);
  protected readonly push = inject(FirebaseMessagingService);

  logout(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigateByUrl('/login'),
      error: () => this.router.navigateByUrl('/login'),
    });
  }

  enableNotifications(): void {
    this.push.enable().subscribe();
  }

  disableNotifications(): void {
    this.push.disable().subscribe();
  }

  copyToken(): void {
    const token = this.push.fcmToken();
    if (!token) return;
    navigator.clipboard.writeText(token).catch(() => {
      /* clipboard unavailable — the token is shown inline anyway */
    });
  }
}
