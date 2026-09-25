plugins {
    id("com.android.library")
}

android {
    namespace = "com.deepanjanxyz.notepad.core.ui"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
