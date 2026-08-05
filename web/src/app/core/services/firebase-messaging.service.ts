import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import type { FirebaseApp } from 'firebase/app';
import { environment } from '../../../environments/environment';
import { DeviceService } from './device.service';
import { DeviceTokenResponse, FcmTokenRequest, PushMessage } from '../models/device.models';
import { catchError, from, map, Observable, of, switchMap, tap } from 'rxjs';

// Firebase SDK is imported dynamically so the bundle only loads it when the
// user actually enables notifications.
let messagingModulePromise: Promise<typeof import('firebase/messaging')> | null = null;
let appPromise: Promise<FirebaseApp> | null = null;

@Injectable({ providedIn: 'root' })
export class FirebaseMessagingService {
  private readonly http = inject(HttpClient);
  private readonly device = inject(DeviceService);

  private readonly baseUrl = `${environment.apiUrl}/api/v1/device-tokens`;

  private listening = false;

  // --- UI state ---
  readonly permission = signal<NotificationPermission | 'unsupported' | 'unconfigured'>('unsupported');
  readonly fcmToken = signal<string | null>(null);
  readonly registered = signal(false);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly messages = signal<PushMessage[]>([]);
  readonly supported = computed(() => this.permission() !== 'unsupported');

  /** True only when the user pasted real Firebase values into environment.ts. */
  get configured(): boolean {
    return !environment.firebase.apiKey.startsWith('PASTE_YOUR_') && !environment.vapidKey.startsWith('PASTE_YOUR_');
  }

  constructor() {
    this.permission.set(this.detectPermission());
  }

  /** Inspects Notification API once on startup (does not prompt). */
  private detectPermission(): NotificationPermission | 'unsupported' | 'unconfigured' {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      return 'unsupported';
    }
    if (!this.configured) {
      return 'unconfigured';
    }
    return Notification.permission;
  }

  /** Requests browser permission (the user gesture triggers this). */
  enable(): Observable<void> {
    this.busy.set(true);
    this.error.set(null);

    return from(this.ensureFirebase()).pipe(
      switchMap(() => from(Notification.requestPermission())),
      switchMap((permission) => {
        this.permission.set(permission);
        if (permission !== 'granted') {
          this.busy.set(false);
          this.error.set(
            permission === 'denied'
              ? 'Notifications are blocked. Enable them in your browser site settings.'
              : 'Permission not granted.',
          );
          return of(void 0);
        }
        return this.register();
      }),
      tap(() => this.busy.set(false)),
      catchError((err) => {
        this.busy.set(false);
        this.error.set(err instanceof Error ? err.message : 'Failed to enable notifications.');
        return of(void 0);
      }),
    );
  }

  /** Gets an FCM token (if permission granted) and registers it with the backend. */
  register(): Observable<void> {
    this.busy.set(true);
    this.error.set(null);

    return from(this.getFcmToken()).pipe(
      switchMap((token) => {
        if (!token) return of(void 0);
        this.fcmToken.set(token);
        const body: FcmTokenRequest = {
          deviceId: this.device.deviceId,
          fcmToken: token,
          deviceType: 'WEB',
        };
        return this.http.post<DeviceTokenResponse>(this.baseUrl, body).pipe(map(() => void 0));
      }),
      tap(() => {
        this.registered.set(true);
        this.listenForMessages();
        this.busy.set(false);
      }),
      catchError((err) => {
        this.busy.set(false);
        this.error.set(err instanceof Error ? err.message : 'Failed to register token.');
        return of(void 0);
      }),
    );
  }

  /** Unregisters: removes the token from the backend and from Firebase. */
  disable(): Observable<void> {
    this.busy.set(true);
    this.error.set(null);

    return this.unregisterOnBackend().pipe(
      switchMap(() => from(this.deleteFcmToken())),
      tap(() => {
        this.fcmToken.set(null);
        this.registered.set(false);
        this.busy.set(false);
      }),
      catchError((err) => {
        this.busy.set(false);
        this.error.set(err instanceof Error ? err.message : 'Failed to disable notifications.');
        return of(void 0);
      }),
    );
  }

  private unregisterOnBackend(): Observable<void> {
    if (!this.registered()) return of(void 0);
    return this.http.delete<void>(`${this.baseUrl}/${this.device.deviceId}`).pipe(catchError(() => of(void 0)));
  }

  private async getFcmToken(): Promise<string | null> {
    const { getMessaging, getToken } = await this.messagingModule();
    const messaging = getMessaging(await this.app());
    return getToken(messaging, { vapidKey: environment.vapidKey });
  }

  private async deleteFcmToken(): Promise<void> {
    const { getMessaging, deleteToken } = await this.messagingModule();
    const messaging = getMessaging(await this.app());
    await deleteToken(messaging);
  }

  /** Foreground messages (tab open & focused) are delivered to the page. */
  private async listenForMessages(): Promise<void> {
    if (this.listening) return; // avoid duplicate subscriptions
    this.listening = true;
    const { getMessaging, onMessage } = await this.messagingModule();
    const messaging = getMessaging(await this.app());
    onMessage(messaging, (payload) => {
      const next: PushMessage = {
        title: payload.notification?.title ?? 'EZDO',
        body: payload.notification?.body ?? '',
        data: payload.data as Record<string, string> | undefined,
      };
      this.messages.update((prev) => [next, ...prev].slice(0, 20));
    });
  }

  private async ensureFirebase(): Promise<void> {
    if (!this.configured) {
      throw new Error('Firebase is not configured. Paste your config into src/environments/environment.ts first.');
    }
    await this.app();
  }

  private app(): Promise<FirebaseApp> {
    if (!appPromise) {
      appPromise = import('firebase/app').then(({ initializeApp }) => initializeApp(environment.firebase));
    }
    return appPromise;
  }

  private messagingModule(): Promise<typeof import('firebase/messaging')> {
    messagingModulePromise ??= import('firebase/messaging');
    return messagingModulePromise;
  }
}
