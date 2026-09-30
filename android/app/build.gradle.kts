plugins { id("com.android.application") }

android {
    namespace = "com.example.texttopdf"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.texttopdf"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { checkReleaseBuilds = false; abortOnError = false }

    val storeFile = providers.environmentVariable("RELEASE_STORE_FILE").orNull
    val storePass = providers.environmentVariable("RELEASE_STORE_PASSWORD").orNull
    val alias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
    val keyPass = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
    if (!storeFile.isNullOrBlank() && !storePass.isNullOrBlank() && !alias.isNullOrBlank() && !keyPass.isNullOrBlank()) {
        signingConfigs {
            create("release") {
                this.storeFile = file(storeFile); storePassword = storePass; keyAlias = alias; keyPassword = keyPass
            }
        }
        buildTypes { release { signingConfig = signingConfigs.getByName("release") } }
    }
}

dependencies { implementation("androidx.webkit:webkit:1.12.1") }
