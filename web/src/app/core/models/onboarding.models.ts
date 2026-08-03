export interface OnboardingRequest {
  firstName: string;
  lastName: string;
  birthDate: string; // yyyy-MM-dd
  timezone: string;
  preferredSessionDuration: number; // minutes
  bufferBetweenSessions: number; // minutes
  wakeupTime: string; // HH:mm:ss
  sleepTime: string; // HH:mm:ss
}

export interface PreferencesResponse {
  timezone: string;
  preferredSessionDuration: number;
  bufferBetweenSessions: number;
  wakeupTime: string;
  sleepTime: string;
  schedulingType?: string;
}

export interface UserProfileResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  points: number;
  streak: number;
  maxStreak: number;
  preferences: PreferencesResponse;
}
