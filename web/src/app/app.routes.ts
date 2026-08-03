import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import {onboardingGuard} from './core/guards/onboarding.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'onboarding',
    canActivate: [onboardingGuard],
    loadComponent: () => import('./features/onboarding/onboarding.component').then((m) => m.OnboardingComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent),
  },
];
