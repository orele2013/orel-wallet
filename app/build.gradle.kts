import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}
val releaseSigningFile = rootProject.file(providers.environmentVariable("OREL_SIGNING_PROPERTIES").orElse(".tools/signing/release-signing.properties").get())
val releaseSigning = Properties().apply { if(releaseSigningFile.isFile) releaseSigningFile.inputStream().use {load(it)} }
android {
    namespace = "com.orel.wallet"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.orel.wallet"
        minSdk = 30
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testProguardFiles("proguard-test-rules.pro")
    }
    testBuildType = providers.gradleProperty("orelTestBuildType").orElse("debug").get()
    if(releaseSigningFile.isFile) signingConfigs {
        create("personalRelease") {
            storeFile = rootProject.file(releaseSigning.getProperty("storeFile"))
            storePassword = releaseSigning.getProperty("storePassword")
            keyAlias = releaseSigning.getProperty("keyAlias")
            keyPassword = releaseSigning.getProperty("keyPassword")
        }
    }
    buildTypes {
        debug {
            versionNameSuffix = "-demo"
            buildConfigField("boolean", "DEMO_MODE", "true")
            manifestPlaceholders["demoHceEnabled"] = "true"
            resValue("string", "app_label", "Orel Wallet Demo")
        }
        release {
            applicationIdSuffix = ".companion"
            buildConfigField("boolean", "DEMO_MODE", "false")
            manifestPlaceholders["demoHceEnabled"] = "false"
            resValue("string", "app_label", "Orel Wallet")
            // Test-only opt-out: Compose instrumentation needs APIs removed by app-only R8 analysis.
            val releaseUiTests = providers.gradleProperty("orelReleaseUiTests").orElse("false").get().toBoolean()
            isMinifyEnabled = !releaseUiTests
            isShrinkResources = !releaseUiTests
            if(releaseSigningFile.isFile) signingConfig = signingConfigs.getByName("personalRelease")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isReturnDefaultValues = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    lint { abortOnError = true }
}
kapt { correctErrorTypes = true; arguments { arg("room.schemaLocation", "$projectDir/schemas") } }
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
