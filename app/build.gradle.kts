import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.wineselector.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wineselector.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "2.0"
    }

    // Release signing is optional: the keystore only exists on the maintainer's
    // machine. Without it, debug builds (and CI) still work.
    val keystoreDir = File(System.getProperty("user.home"), "documents/sync/personal/keystore")
    val keystorePasswords = File(keystoreDir, "wine-selector.release.pswd")
    val hasReleaseKeystore = keystorePasswords.exists()

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                val props = Properties()
                keystorePasswords.inputStream().use { props.load(it) }
                storeFile = File(keystoreDir, "wine-selector.release.keystore")
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)

    // CameraX 1.5.3 + ML Kit 16.0.1 ship 16 KB-aligned native libraries (Play requirement).
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.text.recognition)

    implementation(libs.coil.compose)

    testImplementation(libs.junit)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
