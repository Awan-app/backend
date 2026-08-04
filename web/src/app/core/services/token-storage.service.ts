import { Injectable, signal } from '@angular/core';
import { AuthUser } from '../models/auth.models';

const REFRESH_TOKEN_KEY = 'ezdo_refresh_token';
const USER_KEY = 'ezdo_user';

@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  // Access token only ever lives in memory - never persisted to storage.
  readonly accessToken = signal<string | null>(null);
  readonly accessTokenExpiresAt = signal<number | null>(null);

  setAccessToken(token: string, expiresInSeconds: number): void {
    this.accessToken.set(token);
    this.accessTokenExpiresAt.set(Date.now() + expiresInSeconds * 1000);
  }

  clearAccessToken(): void {
    this.accessToken.set(null);
    this.accessTokenExpiresAt.set(null);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  setRefreshToken(token: string): void {
    localStorage.setItem(REFRESH_TOKEN_KEY, token);
  }

  clearRefreshToken(): void {
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }

  // id/email/isNew are not secrets — safe to persist so a page reload doesn't
  // strand an otherwise-valid session while the access token stays memory-only.
  getUser(): AuthUser | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  }

  setUser(user: AuthUser): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  clearUser(): void {
    localStorage.removeItem(USER_KEY);
  }

  clearAll(): void {
    this.clearAccessToken();
    this.clearRefreshToken();
    this.clearUser();
  }
}
