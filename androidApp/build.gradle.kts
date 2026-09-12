plugins {
    id("com.android.application")
}

android {
    namespace = "com.slipmesh.android"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.slipmesh.android"
        minSdk = 26
        targetSdk = 37

        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":"))

    testImplementation("junit:junit:4.13.2")
}
