export interface OtpRequest {
  email: string;
}

export interface OtpResponse {
  expiresInSeconds: number;
  resendAvailableInSeconds: number;
}

export interface VerifyOtpRequest {
  email: string;
  code: string;
  deviceId: string; // UUID
}

export interface AuthUser {
  id: string;
  email: string;
  isNew: boolean;
}

/** Returned by every login path — POST /auth/otp/verify and POST /auth/firebase alike. */
export interface AuthResponse {
  accessToken: string;
  accessTokenExpiresIn: number;
  refreshToken: string;
  user: AuthUser;
}

/** @deprecated Kept as an alias; the backend returns the same shape for both login paths. */
export type VerifyOtpResponse = AuthResponse;

export interface FirebaseLoginRequest {
  idToken: string;
  deviceId: string; // UUID
}

export interface RefreshRequest {
  refreshToken: string;
  deviceId: string; // UUID
}

export interface RefreshResponse {
  accessToken: string;
  accessTokenExpiresIn: number;
  refreshToken: string;
}

export interface LogoutRequest {
  deviceId: string; // UUID
}

// Matches GlobalExceptionHandler.buildErrorResponse
export interface ApiErrorResponse {
  message: string;
  statusCode: number;
  errorCode: string;
  info: Record<string, unknown>;
  timestamp: string;
}

export const ErrorCode = {
  OTP_RATE_LIMIT_EXCEEDED: 'OTP_RATE_LIMIT_EXCEEDED',
  OTP_EXPIRED_OR_NOT_FOUND: 'OTP_EXPIRED_OR_NOT_FOUND',
  OTP_INVALID_CODE: 'OTP_INVALID_CODE',
  OTP_LOCKED: 'OTP_LOCKED',
  REFRESH_TOKEN_INVALID: 'REFRESH_TOKEN_INVALID',
  REFRESH_TOKEN_EXPIRED: 'REFRESH_TOKEN_EXPIRED',
  REFRESH_TOKEN_REUSE_DETECTED: 'REFRESH_TOKEN_REUSE_DETECTED',
  FIREBASE_NOT_CONFIGURED: 'FIREBASE_NOT_CONFIGURED',
  FIREBASE_TOKEN_INVALID: 'FIREBASE_TOKEN_INVALID',
  FIREBASE_PROVIDER_NOT_ALLOWED: 'FIREBASE_PROVIDER_NOT_ALLOWED',
  FIREBASE_EMAIL_MISSING: 'FIREBASE_EMAIL_MISSING',
  FIREBASE_EMAIL_NOT_VERIFIED: 'FIREBASE_EMAIL_NOT_VERIFIED',
  VALIDATION_ERROR: 'VALIDATION_ERROR',
} as const;
