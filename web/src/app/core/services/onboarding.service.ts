import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, finalize, Observable, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { ApiErrorResponse } from '../models/auth.models';
import { OnboardingRequest, UserProfileResponse } from '../models/onboarding.models';

@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/onboarding`;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  complete(request: OnboardingRequest): Observable<UserProfileResponse> {
    this.loading.set(true);
    this.error.set(null);

    return this.http.post<UserProfileResponse>(this.baseUrl, request).pipe(
      tap((profile) => this.auth.applyOnboardedProfile(profile)),
      catchError((err: HttpErrorResponse) => {
        const body = err.error as ApiErrorResponse | undefined;
        this.error.set(body?.message ?? 'Something went wrong. Please try again.');
        return throwError(() => err);
      }),
      finalize(() => this.loading.set(false)),
    );
  }
}
