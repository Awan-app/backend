// Service worker for Firebase Cloud Messaging (Web push).
// Served at the site root by Angular (public/ assets are copied verbatim).
//
// IMPORTANT: paste the SAME Firebase web-app config here as in
// src/environments/environment.ts.
importScripts('https://www.gstatic.com/firebasejs/10.14.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.14.1/firebase-messaging-compat.js');

const firebaseConfig = {
  apiKey: "AIzaSyDN5E-j44-2E9VRltIfnouqmekgz1DL3xY",
  authDomain: "awan-479f9.firebaseapp.com",
  projectId: "awan-479f9",
  storageBucket: "awan-479f9.firebasestorage.app",
  messagingSenderId: "786321941221",
  appId: "1:786321941221:web:ca6e30c25869511613ea0c",
  measurementId: "G-F65GWWPR1D"
};

firebase.initializeApp(firebaseConfig);

const messaging = firebase.messaging();

// Push arrives while the tab/page is NOT in the foreground → show a system notification.
messaging.onBackgroundMessage((payload) => {
  console.log('[firebase-messaging-sw] Background message received: ', payload);

  const notificationTitle = payload.notification?.title ?? 'EZDO';
  const notificationOptions = {
    body: payload.notification?.body ?? '',
    icon: payload.notification?.icon ?? '/favicon.ico',
    data: payload.data ?? {},
    tag: payload.data?.sessionId ?? 'ezdo',
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});

// Tapping the notification opens the app.
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const url = event.notification.data?.url ?? '/';
  event.waitUntil(clients.openWindow(url));
});
