plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "pt.tiago.kaboom"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "pt.tiago.kaboom"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "1.0.0"
    }

    // Assinatura do APK publicado: a chave e as passwords ficam fora do repositório,
    // em ~/.gradle/gradle.properties (KABOOM_STORE_FILE, KABOOM_STORE_PASSWORD,
    // KABOOM_KEY_ALIAS, KABOOM_KEY_PASSWORD). Sem elas, o release sai por assinar.
    val storeFile = providers.gradleProperty("KABOOM_STORE_FILE").orNull
    signingConfigs {
        if (storeFile != null) {
            create("release") {
                this.storeFile = file(storeFile)
                storePassword = providers.gradleProperty("KABOOM_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("KABOOM_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("KABOOM_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            if (storeFile != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")
}
