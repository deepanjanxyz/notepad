import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
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

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.6.1")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.6.1")
    implementation("io.ktor:ktor-client-android:2.3.12")
    testImplementation("junit:junit:4.13.2")
}
