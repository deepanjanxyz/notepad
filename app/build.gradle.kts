import java.io.FileInputStream
import java.util.Properties
import org.gradle.api.GradleException

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
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
    implementation(project(":core"))
    implementation(project(":features"))

    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.biometric:biometric:1.4.0-alpha07")

    // Room Database
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    testImplementation("junit:junit:4.13.2")
}
