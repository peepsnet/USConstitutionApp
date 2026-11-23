import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing details come from keystore.properties at the project root, or from the
// environment (KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD), which is what
// build-release.ps1 uses so that no password is ever written to a file. The keystore and
// keystore.properties are private and never part of the project. See RELEASE.md.
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signingSetting(property: String, environment: String): String? =
    keystoreProps.getProperty(property) ?: System.getenv(environment)

val releaseStoreFile = signingSetting("storeFile", "KEYSTORE_FILE")
val releaseStorePassword = signingSetting("storePassword", "KEYSTORE_PASSWORD")
val releaseKeyAlias = signingSetting("keyAlias", "KEY_ALIAS")
val releaseKeyPassword = signingSetting("keyPassword", "KEY_PASSWORD")
val canSignRelease = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { !it.isNullOrBlank() }

android {
    namespace = "com.example.usconstitution"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.usconstitution"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "3.0.0"
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Without signing details the release build is produced unsigned.
            if (canSignRelease) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material-icons-core")
    // Group and toolbar icons. Release builds keep only the icons that are used (R8).
    implementation("androidx.compose.material:material-icons-extended")
}
