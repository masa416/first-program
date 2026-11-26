plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.masa.idlehome"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.masa.idlehome"
        minSdk = 28
        targetSdk = 34
        versionCode = 3
        versionName = "0.3"
    }

    // 毎回同じ鍵で署名して、上書きインストールできるようにする（試作用の鍵）
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("release") {
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
}
