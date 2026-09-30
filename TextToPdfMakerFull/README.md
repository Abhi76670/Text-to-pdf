# Text to PDF Maker

Native Android app using Kotlin, Jetpack Compose and Material 3.

Toolchain: AGP 8.7.3, Kotlin 2.0.21, Gradle 8.10.2, JDK 17, compileSdk/targetSdk 35, Compose BOM 2024.12.01.

The project does not contain a signing keystore. Configure GitHub secrets `ANDROID_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` for signed releases.

The named font picker is included with offline-safe Android Typeface fallbacks. For exact named typefaces, add properly licensed `.ttf` files under `app/src/main/assets/fonts/` and wire them to `FontChoice`.
