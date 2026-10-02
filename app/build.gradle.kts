import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.google.services)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localOrEnv(key: String): String? = localProps.getProperty(key) ?: System.getenv(key)

fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "app.menosan.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.menosan.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 5
        versionName = "1.3.1"
    }

    flavorDimensions += "env"
    productFlavors {
        create("staging") {
            dimension = "env"
            versionNameSuffix = "-staging"
            buildConfigField(
                "String", "API_BASE_URL",
                quoted(localOrEnv("API_BASE_URL_STAGING") ?: "https://menosan-api-staging.onrender.com/"),
            )
            buildConfigField("long", "HTTP_CONNECT_TIMEOUT_SECONDS", "75L")
            buildConfigField("long", "HTTP_READ_TIMEOUT_SECONDS", "75L")
            buildConfigField("long", "HTTP_CALL_TIMEOUT_SECONDS", "120L")
            resValue("string", "app_name", "Menosan (staging)")
        }
        create("prod") {
            dimension = "env"
            buildConfigField(
                "String", "API_BASE_URL",
                quoted(localOrEnv("API_BASE_URL_PROD") ?: "https://menosan-api.onrender.com/"),
            )
            buildConfigField("long", "HTTP_CONNECT_TIMEOUT_SECONDS", "75L")
            buildConfigField("long", "HTTP_READ_TIMEOUT_SECONDS", "75L")
            buildConfigField("long", "HTTP_CALL_TIMEOUT_SECONDS", "120L")
            resValue("string", "app_name", "Menosan")
        }
    }

    val keystorePath = localOrEnv("MENOSAN_KEYSTORE_FILE")
    val releaseSigning = if (keystorePath != null) {
        signingConfigs.create("release") {
            storeFile = file(keystorePath)
            storePassword = localOrEnv("MENOSAN_KEYSTORE_PASSWORD")
            keyAlias = localOrEnv("MENOSAN_KEY_ALIAS")
            keyPassword = localOrEnv("MENOSAN_KEY_PASSWORD")
        }
    } else {
        null
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = releaseSigning
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime.ktx)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.androidx.exifinterface)
}
