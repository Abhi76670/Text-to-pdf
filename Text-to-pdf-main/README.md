# Text to PDF Maker
TypeScript (Vite) UI in a native Android WebView shell (Java) under `android/`.
Build: `npm install && npm run build` (writes UI into `android/app/src/main/assets/www`), then `gradle -p android :app:assembleRelease`.
Or push to GitHub and run Actions > Build and Release APK. Secrets: ANDROID_KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD.
Fonts come from @fontsource npm packages and are bundled into the APK (offline).
