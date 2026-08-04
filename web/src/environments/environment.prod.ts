export const environment = {
  production: true,
  apiUrl: 'https://api.yourdomain.com',
  // From Firebase console → Project settings → General → Your apps → web app.
  // These are public identifiers, not secrets — safe to commit and to ship in the bundle.
  // Leave apiKey empty to hide the "Continue with Google" button.
  firebase: {
    apiKey: '',
    authDomain: '',
    projectId: '',
    appId: '',
  },
};
