import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)

    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)

            implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
            implementation("com.google.firebase:firebase-firestore-ktx:24.9.1")
            implementation("com.google.firebase:firebase-storage-ktx:20.3.0")
            implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")


            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

            implementation(libs.koin.android)


            implementation("com.google.zxing:core:3.5.1")
            implementation("com.journeyapps:zxing-android-embedded:4.3.0")


            implementation("androidx.compose.material:material-icons-core:1.6.0")
            implementation("androidx.compose.material:material-icons-extended:1.6.0")

            implementation("androidx.camera:camera-camera2:1.3.1")
            implementation("androidx.camera:camera-lifecycle:1.3.1")
            implementation("androidx.camera:camera-view:1.3.1")
            implementation("androidx.camera:camera-extensions:1.3.1")
            implementation("androidx.camera:camera-core:1.3.1")

            implementation("com.google.mlkit:barcode-scanning:17.2.0")
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation("io.ktor:ktor-client-logging:${libs.versions.ktor.get()}")
            implementation("io.ktor:ktor-client-auth:${libs.versions.ktor.get()}")

            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

            implementation(libs.kamel)
            implementation(libs.koin.core)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.koin)

            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
        }
    }
}

android {
    namespace = "com.jetbrains.kmpapp"
    compileSdk = 35

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.jetbrains.kmpapp"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(libs.androidx.compose.ui.tooling)
    }
}
dependencies {
    implementation(libs.androidx.compose.material.core)
    implementation(libs.ui.android)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.room.compiler)

    configurations.all {
        resolutionStrategy {
            force("org.jetbrains:annotations:23.0.0")
            exclude(group = "com.intellij", module = "annotations")
        }
    }
}
