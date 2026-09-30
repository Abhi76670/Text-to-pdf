# Text to PDF Maker

Native Android app using Kotlin, Jetpack Compose and Material 3.

## Project layout

This ZIP is already arranged for direct upload to the **root of a GitHub repository**. Do not put the contents inside another `TextToPdfMakerFull` folder.

```text
.github/workflows/release/apk-build.yml
app/
build.gradle.kts
settings.gradle.kts
gradle.properties
```

## Build toolchain

- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- Gradle 8.9
- JDK 17
- compileSdk 35 / targetSdk 35
- Jetpack Compose BOM 2024.12.01

## GitHub Actions signing secrets

Add these repository **Actions secrets**:

- `ANDROID_KEYSTORE_BASE64` — the complete Base64 text from your keystore TXT file
- `KEYSTORE_PASSWORD` — keystore/container password
- `KEY_ALIAS` — signing key alias
- `KEY_PASSWORD` — signing key password

The workflow installs Android platform 35 and Build Tools 35.0.0, decodes the keystore only inside the runner, builds a signed release APK, verifies the signature, and uploads the APK as an artifact. A tag such as `v1.0.0` also creates a GitHub Release.

## Local build

If Gradle 8.9 is installed:

```bash
gradle :app:assembleDebug
```

For a signed release locally, provide the four `RELEASE_*` environment variables used by the Gradle signing configuration.


### CI build
The release workflow uses JDK 17 and Gradle 8.9, installs Android SDK 35/build-tools 35.0.0, and compiles both Java and Kotlin with JVM target 17. This avoids the Java 8 vs Kotlin 17 target mismatch that can fail `:app:compileReleaseKotlin`.
