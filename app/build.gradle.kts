import java.util.Properties
import java.util.Base64
import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    kotlin("kapt")
}

val backendProperties = Properties().apply {
    val configFile = rootProject.file("local.properties")
    if (configFile.exists()) configFile.inputStream().use { load(it) }
}
fun backendValue(name: String): String = providers.environmentVariable(name).orNull
    ?: backendProperties.getProperty(name, "")
fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
val backendUrl = backendValue("SUPABASE_URL")
val backendKey = backendValue("SUPABASE_ANON_OR_PUBLISHABLE_KEY")
require(backendUrl.startsWith("https://") && !backendUrl.contains("your-supabase-project") &&
    backendUrl.none { it.isWhitespace() }) { "Set a valid SUPABASE_URL in local.properties or the build environment." }
val isPublicKey = if (backendKey.startsWith("sb_publishable_")) backendKey.length > 20 else runCatching {
    val claims = JsonSlurper().parseText(String(Base64.getUrlDecoder().decode(backendKey.split('.')[1]))) as Map<*, *>
    claims["role"] == "anon"
}.getOrDefault(false)
require(isPublicKey && backendKey.none { it.isWhitespace() }) {
    "SUPABASE_ANON_OR_PUBLISHABLE_KEY must be a client anon/publishable key. Privileged keys are forbidden."
}

android {
    namespace = "com.example.ssmapplication3"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ssmapplication3"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", quoted(backendUrl))
        buildConfigField("String", "SUPABASE_ANON_KEY", quoted(backendKey))
        buildConfigField("String", "ENVIRONMENT_NAME", "\"SHARED\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)

    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
