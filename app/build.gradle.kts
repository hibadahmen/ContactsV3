import java.util.Properties
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val secrets = Properties()
rootProject.file("secrets.properties")
    .takeIf { it.isFile }
    ?.inputStream()
    ?.use(secrets::load)

val mapsKey = providers.environmentVariable("MAPS_API_KEY").orNull
    ?: secrets.getProperty("MAPS_API_KEY", "")

android {
    namespace = "tn.isetdjerba.contactsv3"
    compileSdk = 35

    defaultConfig {
        applicationId = "tn.isetdjerba.contactsv3"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "3.0"

        manifestPlaceholders["mapsApiKey"] =
            mapsKey.ifBlank { "MISSING_MAPS_API_KEY" }

        buildConfigField(
            "boolean",
            "MAPS_CONFIGURED",
            mapsKey.isNotBlank().toString()
        )
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
}