import java.io.FileInputStream
import java.util.Properties
import org.gradle.api.GradleException

plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localPropFile = projectDir.resolve("local.properties")
    if (localPropFile.isFile) {
        FileInputStream(localPropFile).use { load(it) }
    }
}

val envProperties = Properties().apply {
    val envFile = projectDir.resolve(".env")
    if (envFile.isFile) {
        envFile.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                put(parts[0].trim(), parts[1].trim().removeSurrounding("\"").removeSurrounding("'"))
            }
        }
    }
}

fun findConfig(key: String): String? = sequenceOf(
    envProperties.getProperty(key),
    localProperties.getProperty(key),
    System.getenv(key)
).mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.firstOrNull()

android {
    namespace = "com.deepanjanxyz.notepad"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.deepanjanxyz.notepad"
        minSdk = 24
        targetSdk = 37
        versionCode = 11
        versionName = "1.0.11"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val releaseKeystore = projectDir.resolve("keystore.jks")
    val expectedKeystore = projectDir.canonicalFile.resolve("keystore.jks")
    if (!releaseKeystore.isFile || releaseKeystore.canonicalFile != expectedKeystore) {
        throw GradleException(
            "Release signing keystore is missing or invalid. Expected a regular keystore at " +
                "${projectDir.resolve("keystore.jks").absolutePath}, strictly inside the app/ directory."
        )
    }

    fun requiredSigningValue(key: String): String = findConfig(key)
        ?: throw GradleException(
            "Missing required release signing value '$key'. Set it in app/.env, " +
                "app/local.properties, or the '$key' system environment variable."
        )

    val releaseStorePass = requiredSigningValue("NOTEPAD_STORE_PASSWORD")
    val releaseAlias = requiredSigningValue("NOTEPAD_KEY_ALIAS")
    val releaseKeyPass = requiredSigningValue("NOTEPAD_KEY_PASSWORD")

    signingConfigs {
        create("release") {
            storeFile = releaseKeystore
            storePassword = releaseStorePass
            keyAlias = releaseAlias
            keyPassword = releaseKeyPass
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:notes"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:drawing"))
    implementation(project(":feature:settings"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.biometric)

    testImplementation(libs.junit)
}
