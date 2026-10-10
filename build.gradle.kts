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
    id("androidx.room") version "2.8.5" apply false
    id("dev.detekt") version "2.0.0-alpha.6" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

// Static analysis (detekt) for every module. The plugin is applied per module so
// that `./gradlew detekt` analyses each one, and all modules share the single
// configuration and baseline under config/detekt/. The version tracks the project
// toolchain (Kotlin 2.4, AGP 9, Gradle 9), which detekt 2.0.0-alpha.6 is built against.
subprojects {
    apply(plugin = "dev.detekt")

    configure<dev.detekt.gradle.extensions.DetektExtension> {
        buildUponDefaultConfig.set(true)
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        baseline.set(rootProject.file("config/detekt/baseline.xml"))
    }

    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        jvmTarget.set("21")
    }
}
