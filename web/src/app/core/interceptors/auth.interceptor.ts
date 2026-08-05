import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { TokenStorageService } from '../services/token-storage.service';

// Endpoints that mint a session. A 401 from these means bad credentials, not an expired
// access token, so the refresh-and-replay below must not fire for them.
const AUTH_ENDPOINTS = ['/otp/request', '/otp/verify', '/firebase', '/refresh'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokens = inject(TokenStorageService);
  const authService = inject(AuthService);

  const isAuthEndpoint = AUTH_ENDPOINTS.some((path) => req.url.includes(path));
  const accessToken = tokens.accessToken();

  const authedReq = accessToken && !isAuthEndpoint
    ? req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } })
    : req;

  return next(authedReq).pipe(
    catchError((err: unknown) => {
      const isUnauthorized = err instanceof HttpErrorResponse && err.status === 401;
      if (!isUnauthorized || isAuthEndpoint) {
        return throwError(() => err);
      }

      // Try one refresh, then replay the original request once.
      return authService.refresh().pipe(
        switchMap((newAccessToken) => {
          const retried = req.clone({ setHeaders: { Authorization: `Bearer ${newAccessToken}` } });
          return next(retried);
        }),
      );
    }),
  );
};
