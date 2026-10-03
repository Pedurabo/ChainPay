import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")

    if (file.exists()) {
        file.inputStream().use {
            load(it)
        }
    }
}

val reownProjectId =
    localProperties.getProperty(
        "REOWN_PROJECT_ID",
        ""
    )

android {
    namespace = "com.chainpay.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.chainpay.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 9
        versionName = "0.9.0"

        buildConfigField(
            "String",
            "REOWN_PROJECT_ID",
            "\"$reownProjectId\""
        )
    }
    buildTypes {
        release {
            // Keep shrinking off until the Reown/payment stack has dedicated
            // release-mode ProGuard/R8 regression coverage.
            isMinifyEnabled = false
            isShrinkResources = false
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
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation("com.google.zxing:core:3.5.4")

    val composeBom =
        platform(
            "androidx.compose:compose-bom:2026.09.00"
        )

    implementation(composeBom)

    implementation(
        "androidx.activity:activity-compose:1.13.0"
    )

    implementation(
        "androidx.compose.ui:ui"
    )

    implementation(
        "androidx.compose.ui:ui-tooling-preview"
    )

    implementation(
        "androidx.compose.material3:material3"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0"
    )

    implementation(
        platform("com.reown:android-bom:1.6.17")
    )

    implementation(
        "com.reown:android-core"
    )

    implementation(
        "com.reown:appkit"
    )

    
    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-navigation")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )
}
