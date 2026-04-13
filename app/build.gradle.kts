import java.util.Properties
import org.gradle.api.GradleException

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

val signingLocalProperties = Properties()
val signingLocalPropertiesFile = rootProject.file("local.properties")
if (signingLocalPropertiesFile.exists()) {
    signingLocalPropertiesFile.inputStream().use { input ->
        signingLocalProperties.load(input)
    }
}

fun signingValue(key: String): String? {
    return System.getenv(key)
        ?.takeIf { value -> value.isNotBlank() }
        ?: signingLocalProperties.getProperty(key)
            ?.takeIf { value -> value.isNotBlank() }
}

fun normalizedKeystorePath(): String? {
    return signingValue("KEYSTORE_FILE")
        ?.replace("\\\\:", ":")
        ?.replace("\\\\", "\\")
}

fun validateReleaseSigningConfig() {
    val requiredKeys = listOf(
        "KEYSTORE_FILE",
        "KEYSTORE_PASSWORD",
        "KEY_ALIAS",
        "KEY_PASSWORD"
    )
    val missingKeys = requiredKeys.filter { signingValue(it).isNullOrBlank() }
    if (missingKeys.isNotEmpty()) {
        throw GradleException(
            "Release and sideload builds require signing values for: " +
                missingKeys.joinToString(", ") +
                ". Set them in environment variables or local.properties."
        )
    }

    val keystorePath = normalizedKeystorePath()
        ?: throw GradleException(
            "KEYSTORE_FILE is required for release and sideload builds."
        )
    if (!file(keystorePath).exists()) {
        throw GradleException(
            "Release keystore not found at '$keystorePath'. " +
                "Update KEYSTORE_FILE in local.properties or environment variables."
        )
    }
}

android {
    namespace = "com.tradesketch.estimator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tradesketch.estimator"
        minSdk = 26
        targetSdk = 35
        versionCode = 24
        versionName = "1.0.22"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            debugSymbolLevel = "SYMBOL_TABLE"
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFilePath = normalizedKeystorePath()
            if (keystoreFilePath != null) {
                storeFile = file(keystoreFilePath)
                storePassword = signingValue("KEYSTORE_PASSWORD")
                keyAlias = signingValue("KEY_ALIAS")
                keyPassword = signingValue("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig?.storeFile != null) {
                signingConfig = releaseSigningConfig
            }
        }
        create("sideload") {
            initWith(getByName("release"))
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            resValue("string", "app_name", "TradeSketch Estimator Local")
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig?.storeFile != null) {
                signingConfig = releaseSigningConfig
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

gradle.taskGraph.whenReady {
    val requiresSignedBuild = allTasks.any { task ->
        val taskName = task.name.lowercase()
        taskName.contains("release") || taskName.contains("sideload")
    }
    if (requiresSignedBuild) {
        validateReleaseSigningConfig()
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(project(":core"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.12.0")

    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

