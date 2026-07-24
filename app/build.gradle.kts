plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "software.kanunnikoff.izhitsa"
    compileSdk = 37

    defaultConfig {
        applicationId = "software.kanunnikoff.izhitsa"
        minSdk = 26
        targetSdk = 37
        versionCode = 6
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            optimization {
                enable = true
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        noCompress += "bin"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.google.material)
    implementation(libs.billing.client)
    implementation(libs.play.review)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.savedstate.ktx)
    implementation(libs.androidx.emoji2.emojipicker)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
