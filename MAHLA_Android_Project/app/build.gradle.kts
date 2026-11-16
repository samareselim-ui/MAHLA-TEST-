plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.hiberwana7la.mahla"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hiberwana7la.mahla"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1-test"
    }

    flavorDimensions += "edition"
    productFlavors {
        create("owner") {
            dimension = "edition"
            applicationIdSuffix = ".owner"
            versionNameSuffix = "-owner"
            resValue("string", "app_name", "مهلة - المالكة")
        }
        create("client") {
            dimension = "edition"
            applicationIdSuffix = ".client"
            versionNameSuffix = "-client"
            resValue("string", "app_name", "مهلة")
        }
    }

    signingConfigs {
        create("release") {
            val releaseStoreFile = System.getenv("MAHLA_KEYSTORE_FILE")
            val releaseStorePassword = System.getenv("MAHLA_KEYSTORE_PASSWORD")
            val releaseKeyAlias = System.getenv("MAHLA_KEY_ALIAS")
            val releaseKeyPassword = System.getenv("MAHLA_KEY_PASSWORD")

            if (!releaseStoreFile.isNullOrBlank() &&
                !releaseStorePassword.isNullOrBlank() &&
                !releaseKeyAlias.isNullOrBlank() &&
                !releaseKeyPassword.isNullOrBlank()
            ) {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.12.1")
}
