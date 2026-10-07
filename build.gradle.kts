// Top-level build file where you can add configuration options common to all sub-projects/modules.

// AGP 9.x ships built-in Kotlin pinned to KGP 2.2.10, whose compiler can only read
// Kotlin metadata up to 2.3.0. Dependencies compiled with Kotlin 2.4.x (e.g.
// supabase-kt 3.8.0) therefore fail with "Module was compiled with an incompatible
// version of Kotlin". Putting a newer KGP on the buildscript classpath is the
// documented way to move AGP's built-in Kotlin forward.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}

plugins {
    id("com.android.application") version "9.4.1" apply false
    id("com.android.library") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
