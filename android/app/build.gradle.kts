plugins {
    alias(libs.plugins.android.application)
}

val apiBaseUrlOverride = providers.gradleProperty("apiBaseUrl")
val googleWebClientIdOverride = providers.gradleProperty("googleWebClientId")

// Hosted demo backend (Laravel API + admin SPA on the same origin).
// Local servers can still be used with -PapiBaseUrl="http://10.0.2.2:8000/api/v1".
val defaultApiBaseUrl = "https://eshopverse.wasmer.app/api/v1"

android {
    namespace = "com.averonlabs.eshopverse"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.averonlabs.eshopverse"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${apiBaseUrlOverride.orElse(defaultApiBaseUrl).get()}\""
            )
            buildConfigField(
                "String",
                "GOOGLE_WEB_CLIENT_ID",
                "\"${googleWebClientIdOverride.orElse("828340620853-examplewebclient.apps.googleusercontent.com").get()}\""
            )
        }

        release {
            isMinifyEnabled = false
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${apiBaseUrlOverride.orElse(defaultApiBaseUrl).get()}\""
            )
            buildConfigField(
                "String",
                "GOOGLE_WEB_CLIENT_ID",
                "\"${googleWebClientIdOverride.orElse("828340620853-examplewebclient.apps.googleusercontent.com").get()}\""
            )
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        unitTests.all {
            it.jvmArgs("-XX:+EnableDynamicAgentLoading")
        }
    }
}

dependencies {
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.core)
    implementation(libs.fragment)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.swiperefreshlayout)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.glide)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.arch.core.testing)

    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
