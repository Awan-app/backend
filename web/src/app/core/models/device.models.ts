export interface FcmTokenRequest {
  deviceId: string;
  fcmToken: string;
  deviceType: 'WEB' | 'ANDROID' | 'IOS';
}

export interface DeviceTokenResponse {
  id: string;
  deviceId: string;
  deviceType: string;
  createdAt: string;
  updatedAt: string;
}

export interface PushMessage {
  title: string;
  body: string;
  data?: Record<string, string>;
}
