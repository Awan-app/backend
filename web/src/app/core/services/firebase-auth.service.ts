import { Injectable } from '@angular/core';
import { FirebaseApp, initializeApp } from 'firebase/app';
import {
  Auth,
  GoogleAuthProvider,
  getAuth,
  signInWithPopup,
  signOut as firebaseSignOut,
} from 'firebase/auth';
import { environment } from '../../../environments/environment';

/** Thrown when the user closes or cancels the Google popup — not a real failure. */
export class PopupCancelledError extends Error {
  constructor() {
    super('Sign-in cancelled');
  }
}

const CANCELLED_CODES = new Set([
  'auth/popup-closed-by-user',
  'auth/cancelled-popup-request',
  'auth/user-cancelled',
]);

/**
 * Thin wrapper over the Firebase JS SDK. Its only job is to run the Google popup and
 * hand back an ID token — the backend does everything else.
 */
@Injectable({ providedIn: 'root' })
export class FirebaseAuthService {
  private app: FirebaseApp | null = null;
  private auth: Auth | null = null;

  /** False when environment.firebase is unfilled, which hides the Google button. */
  readonly isConfigured = !!environment.firebase?.apiKey;

  /** @returns the Firebase ID token to post to our backend. */
  async signInWithGoogle(): Promise<string> {
    const auth = this.getAuth();
    try {
      const result = await signInWithPopup(auth, new GoogleAuthProvider());
      return await result.user.getIdToken();
    } catch (err) {
      if (CANCELLED_CODES.has((err as { code?: string })?.code ?? '')) {
        throw new PopupCancelledError();
      }
      throw err;
    }
  }

  async signOut(): Promise<void> {
    if (!this.auth) return; // never signed in on this page load
    await firebaseSignOut(this.auth);
  }

  /** Lazy so an app with Firebase unconfigured still boots and the OTP flow keeps working. */
  private getAuth(): Auth {
    if (!this.isConfigured) {
      throw new Error('Firebase is not configured. Fill in environment.firebase.');
    }
    if (!this.auth) {
      this.app = initializeApp(environment.firebase);
      this.auth = getAuth(this.app);
    }
    return this.auth;
  }
}
