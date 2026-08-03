import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { OnboardingService } from '../../core/services/onboarding.service';
import { OnboardingRequest } from '../../core/models/onboarding.models';

@Component({
  selector: 'app-onboarding',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './onboarding.component.html',
})
export class OnboardingComponent {
  private readonly router = inject(Router);
  protected readonly onboarding = inject(OnboardingService);

  protected readonly firstName = signal('');
  protected readonly lastName = signal('');
  protected readonly birthDate = signal('');
  protected readonly timezone = signal(Intl.DateTimeFormat().resolvedOptions().timeZone);
  protected readonly preferredSessionDuration = signal(30);
  protected readonly bufferBetweenSessions = signal(10);
  protected readonly wakeupTime = signal('07:00');
  protected readonly sleepTime = signal('23:00');

  protected readonly formValid = computed(() =>
    this.firstName().trim().length > 0 &&
    this.lastName().trim().length > 0 &&
    this.birthDate().length > 0 &&
    this.timezone().trim().length > 0 &&
    this.preferredSessionDuration() >= 0 &&
    this.bufferBetweenSessions() >= 0 &&
    this.wakeupTime().length > 0 &&
    this.sleepTime().length > 0,
  );

  updatePreferredSessionDuration(value: string): void {
    this.preferredSessionDuration.set(Number(value) || 0);
  }

  updateBufferBetweenSessions(value: string): void {
    this.bufferBetweenSessions.set(Number(value) || 0);
  }

  submit(): void {
    if (!this.formValid() || this.onboarding.loading()) return;

    const request: OnboardingRequest = {
      firstName: this.firstName().trim(),
      lastName: this.lastName().trim(),
      birthDate: this.birthDate(),
      timezone: this.timezone().trim(),
      preferredSessionDuration: this.preferredSessionDuration(),
      bufferBetweenSessions: this.bufferBetweenSessions(),
      wakeupTime: `${this.wakeupTime()}:00`,
      sleepTime: `${this.sleepTime()}:00`,
    };

    console.log(request);

    this.onboarding.complete(request).subscribe({
      next: () => this.router.navigateByUrl('/'),
    });
  }
}
