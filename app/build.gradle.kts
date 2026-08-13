plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Sem este plugin, qualquer @Composable falha na compilacao.
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "br.com.fitcoachvision"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.fitcoachvision"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-fase1"
    }

    // Chave de debug FIXA, versionada no repositorio.
    // Sem isso, cada build do GitHub Actions geraria uma assinatura diferente e o
    // Android recusaria a atualizacao com INSTALL_FAILED_UPDATE_INCOMPATIBLE.
    // Nao use esta chave para publicar na Play Store.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
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

    // Os modelos .task do MediaPipe precisam ficar sem compressao no APK,
    // caso contrario o carregamento do modelo falha em tempo de execucao.
    androidResources {
        noCompress += "task"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    implementation(libs.mediapipe.tasks.vision)
}
