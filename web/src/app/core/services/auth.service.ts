import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, EMPTY, finalize, from, map, Observable, of, switchMap, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenStorageService } from './token-storage.service';
import { DeviceService } from './device.service';
import { FirebaseAuthService, PopupCancelledError } from './firebase-auth.service';
import { UserProfileResponse } from '../models/onboarding.models';

import {
  ApiErrorResponse,
  AuthResponse,
  AuthUser,
  ErrorCode,
  FirebaseLoginRequest,
  OtpRequest,
  OtpResponse,
  RefreshRequest,
  RefreshResponse,
  VerifyOtpRequest,
  LogoutRequest,
} from '../models/auth.models';

export type AuthStep = 'email' | 'otp';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokens = inject(TokenStorageService);
  private readonly device = inject(DeviceService);
  private readonly firebaseAuth = inject(FirebaseAuthService);

  /** Drives whether the login screen offers "Continue with Google". */
  readonly googleAvailable = this.firebaseAuth.isConfigured;

  private readonly baseUrl = `${environment.apiUrl}/api/v1/auth`;
  private readonly usersUrl = `${environment.apiUrl}/api/v1/users`;

  // --- session state ---
  readonly user = signal<AuthUser | null>(null);
  readonly isAuthenticated = computed(() => this.user() !== null && this.tokens.accessToken() !== null);

  // --- login flow state ---
  readonly step = signal<AuthStep>('email');
  readonly email = signal<string>('');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly remainingAttempts = signal<number | null>(null);
  readonly resendRemaining = signal<number>(0);
  readonly profile = signal<UserProfileResponse | null>(null);

  private refreshTimer: ReturnType<typeof setTimeout> | null = null;
  private cooldownTimer: ReturnType<typeof setInterval> | null = null;
  private refreshInFlight$: Observable<string> | null = null;

  requestOtp(email: string): Observable<OtpResponse> {
    this.loading.set(true);
    this.error.set(null);
    this.remainingAttempts.set(null);

    const body: OtpRequest = { email };

    return this.http.post<OtpResponse>(`${this.baseUrl}/otp/request`, body).pipe(
      tap((res) => {
        this.email.set(email);
        this.step.set('otp');
        this.startResendCooldown(res.resendAvailableInSeconds);
      }),
      catchError((err) => this.handleError(err)),
      finalize(() => this.loading.set(false)),
    );
  }

  /**
   * Google sign-in. Firebase handles the popup and returns an ID token, which the backend
   * verifies and swaps for the same session any other login path produces.
   *
   * Completes without emitting if the user closes the popup.
   */
  loginWithGoogle(): Observable<AuthResponse> {
    this.loading.set(true);
    this.error.set(null);
    this.remainingAttempts.set(null);

    return from(this.firebaseAuth.signInWithGoogle()).pipe(
      switchMap((idToken) => {
        const body: FirebaseLoginRequest = { idToken, deviceId: this.device.deviceId };
        return this.http.post<AuthResponse>(`${this.baseUrl}/firebase`, body);
      }),
      tap((res) => this.applySession(res)),
      catchError((err) => {
        if (err instanceof PopupCancelledError) return EMPTY; // user backed out; not an error
        return this.handleError(err);
      }),
      finalize(() => this.loading.set(false)),
    );
  }

  verifyOtp(code: string): Observable<AuthResponse> {
    this.loading.set(true);
    this.error.set(null);
    this.remainingAttempts.set(null);

    const body: VerifyOtpRequest = {
      email: this.email(),
      code,
      deviceId: this.device.deviceId,
    };

    return this.http.post<AuthResponse>(`${this.baseUrl}/otp/verify`, body).pipe(
      tap((res) => this.applySession(res)),
      catchError((err) => this.handleError(err)),
      finalize(() => this.loading.set(false)),
    );
  }

  /** Exchanges the stored refresh token for a new access token. Concurrent callers share one in-flight request. */
  refresh(): Observable<string> {
    if (this.refreshInFlight$) return this.refreshInFlight$;

    const refreshToken = this.tokens.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    const body: RefreshRequest = { refreshToken, deviceId: this.device.deviceId };

    const request$ = this.http.post<RefreshResponse>(`${this.baseUrl}/refresh`, body).pipe(
      tap((res) => {
        this.tokens.setAccessToken(res.accessToken, res.accessTokenExpiresIn);
        this.tokens.setRefreshToken(res.refreshToken);
        this.scheduleRefresh(res.accessTokenExpiresIn);
      }),
      map((res) => res.accessToken),
      catchError((err: HttpErrorResponse) => {
        const errorCode = (err.error as ApiErrorResponse | undefined)?.errorCode;
        this.clearSession();
        if (errorCode === ErrorCode.REFRESH_TOKEN_REUSE_DETECTED) {
          this.error.set(
            'Your session was used from another location and has been signed out everywhere for your security. Please sign in again.',
          );
        }
        return this.handleError(err);
      }),
      finalize(() => {
        this.refreshInFlight$ = null;
      }),
    );

    this.refreshInFlight$ = request$;
    return request$;
  }

  logout(): Observable<void> {
    const body: LogoutRequest = { deviceId: this.device.deviceId };

    return this.http.post<void>(`${this.baseUrl}/logout`, body).pipe(
      catchError(() => of(void 0)), // clear locally even if the network call fails
      tap(() => this.clearSession()),
    );
  }

  resendOtp(): Observable<OtpResponse> | null {
    if (this.resendRemaining() > 0 || !this.email()) return null;
    return this.requestOtp(this.email());
  }

  backToEmail(): void {
    this.step.set('email');
    this.error.set(null);
    this.remainingAttempts.set(null);
    this.stopCooldown();
  }

  /** Call once on app startup to restore a session from the persisted refresh token. */
  tryRestoreSession(): Observable<boolean> {
    const storedUser = this.tokens.getUser();
    const refreshToken = this.tokens.getRefreshToken();

    if (!refreshToken) {
      if (storedUser) this.tokens.clearUser(); // orphaned user with no token to back it
      return of(false);
    }

    if (storedUser) this.user.set(storedUser);

    return this.refresh().pipe(
      // The profile is memory-only, so a reload has to fetch it again or the home
      // screen falls back to showing just the email.
      switchMap(() => this.loadProfile()),
      map(() => true),
      catchError(() => of(false)),
    );
  }

  /**
   * Fetches the profile the home screen renders. Never fails the caller — a profile we
   * could not load just leaves the screen in its reduced state rather than killing the
   * session restore.
   */
  loadProfile(): Observable<UserProfileResponse | null> {
    if (this.user()?.isNew) return of(null); // no profile until onboarding is done

    return this.http.get<UserProfileResponse>(`${this.usersUrl}/me`).pipe(
      tap((profile) => this.profile.set(profile)),
      catchError(() => of(null)),
    );
  }

  /** Called by OnboardingService once the user finishes onboarding. */
  applyOnboardedProfile(profile: UserProfileResponse): void {
    this.profile.set(profile);
    const current = this.user();
    if (current) {
      const updated = { ...current, isNew: false };
      this.user.set(updated);
      this.tokens.setUser(updated);
    }
  }

  private applySession(res: AuthResponse): void {
    this.tokens.setAccessToken(res.accessToken, res.accessTokenExpiresIn);
    this.tokens.setRefreshToken(res.refreshToken);
    this.tokens.setUser(res.user);
    this.user.set(res.user);
    this.scheduleRefresh(res.accessTokenExpiresIn);
    this.stopCooldown();
    // A returning user goes straight to home, which needs the profile. A new user goes to
    // onboarding, which produces it — loadProfile() no-ops for them.
    this.loadProfile().subscribe();
  }

  private clearSession(): void {
    // Drop the Firebase session too, or the next popup silently reuses the old Google account.
    this.firebaseAuth.signOut().catch(() => undefined);
    this.tokens.clearAll();
    this.user.set(null);
    this.profile.set(null);
    this.step.set('email');
    this.email.set('');
    if (this.refreshTimer) clearTimeout(this.refreshTimer);
  }

  private scheduleRefresh(expiresInSeconds: number): void {
    if (this.refreshTimer) clearTimeout(this.refreshTimer);
    const delayMs = Math.max((expiresInSeconds - 60) * 1000, 5_000);
    this.refreshTimer = setTimeout(() => {
      this.refresh().subscribe({ error: () => this.clearSession() });
    }, delayMs);
  }

  private startResendCooldown(seconds: number): void {
    this.stopCooldown();
    this.resendRemaining.set(seconds);
    this.cooldownTimer = setInterval(() => {
      const remaining = this.resendRemaining();
      if (remaining <= 1) {
        this.stopCooldown();
      } else {
        this.resendRemaining.set(remaining - 1);
      }
    }, 1000);
  }

  private stopCooldown(): void {
    if (this.cooldownTimer) clearInterval(this.cooldownTimer);
    this.cooldownTimer = null;
    this.resendRemaining.set(0);
  }

  // `unknown` rather than HttpErrorResponse: the Google path can also fail inside the Firebase SDK.
  private handleError(err: unknown): Observable<never> {
    const body = (err as HttpErrorResponse)?.error as ApiErrorResponse | undefined;
    this.error.set(body?.message ?? 'Something went wrong. Please try again.');

    if (body?.errorCode === ErrorCode.OTP_INVALID_CODE) {
      const remaining = body.info?.['remainingAttempts'];
      this.remainingAttempts.set(typeof remaining === 'number' ? remaining : null);
    } else {
      this.remainingAttempts.set(null);
    }

    return throwError(() => err);
  }
}
