// =============================================================================
// Signing configuration snippet for composeApp/build.gradle.kts
// Place inside the android {} block.
// Reads from keystore.properties locally, environment variables in CI.
//
// GOTCHA: If keystore.properties does not exist AND env vars are empty,
// the signing config will have empty passwords. Gradle will NOT fail at
// configuration time -- it fails at signing time with a cryptic
// "Keystore was tampered with, or password was incorrect" error.
// =============================================================================

signingConfigs {
    create("release") {
        val keystorePropertiesFile = rootProject.file("composeApp/keystore/keystore.properties")
        if (keystorePropertiesFile.exists()) {
            val keystoreProperties = java.util.Properties().apply {
                load(keystorePropertiesFile.inputStream())
            }
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        } else {
            // CI: read from environment variables
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "keystore/release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
}

buildTypes {
    debug {
        isMinifyEnabled = false
        applicationIdSuffix = ".debug"
        versionNameSuffix = "-debug"
    }
    release {
        // CRITICAL: isMinifyEnabled = true WITHOUT correct ProGuard rules
        // causes ClassNotFoundException at runtime. See proguard-rules.pro
        // in assets/templates/ for the complete KMP stack rules.
        isMinifyEnabled = true
        isShrinkResources = true
        signingConfig = signingConfigs.getByName("release")
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
