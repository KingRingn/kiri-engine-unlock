plugins {
    id("com.android.application")
}

android {
    namespace = "com.kiriengine.unlock"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kiriengine.unlock"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Xposed Framework API (compileOnly since it's provided at runtime)
    compileOnly("de.robv.android.xposed:api:82")
    // Add sources jar for IDE reference
    compileOnly("de.robv.android.xposed:api:82:sources")
}
