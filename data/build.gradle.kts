plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

android {
    namespace = "com.deepanjanxyz.notepad.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

}

// Export the Room schema so migrations can be authored for every version bump
// instead of relying on destructive fallback.
//
// Configured through the Room Gradle Plugin rather than a raw
// `ksp { arg("room.schemaLocation", ...) }`. With the raw argument the project
// schema folder is an output of every variant's KSP task, so a parallel build
// (`assembleDebug assembleRelease --parallel`) has one variant read the schema
// file while another is still writing it, which fails with a truncated-JSON
// error. The plugin keeps `$projectDir/schemas` a pure input and writes the
// per-variant schemas into the build directory instead.
room {
    schemaDirectory(path = "$projectDir/schemas")
}

dependencies {
    implementation(project(":domain"))
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("io.github.jan-tennert.supabase:auth-kt:3.8.0")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.8.0")
    implementation("io.ktor:ktor-client-android:3.6.0")
    testImplementation("junit:junit:4.13.2")
}
