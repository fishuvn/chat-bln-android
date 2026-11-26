import com.android.build.api.variant.FilterConfiguration

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val keystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")
val hasCustomKeystore: Boolean = !keystorePath.isNullOrBlank() && file(keystorePath).exists()
val ciVersionName: String? = System.getenv("APP_VERSION_NAME")?.takeIf { it.isNotBlank() }
val ciVersionCode: Int? = System.getenv("APP_VERSION_CODE")?.toIntOrNull()

android {
    namespace = "nhom3.chatbln.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "nhom3.chatbln.android"
        minSdk = 26
        targetSdk = 35
        versionCode = ciVersionCode ?: 10000
        versionName = ciVersionName ?: "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasCustomKeystore) {
            create("releaseSign") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: "chatbln123"
                keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "chatbln"
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: "chatbln123"
            }
        }
    }

    // Xuất đúng 3 file APK: ARM 32-bit (armeabi-v7a), ARM 64-bit (arm64-v8a), và ARM Universal
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasCustomKeystore) {
                signingConfigs.getByName("releaseSign")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val abiCodes = mapOf(
    "armeabi-v7a" to 1,
    "arm64-v8a" to 2
)

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        // Loại bỏ thư viện native x86/x86_64 khỏi bản release để bản universal là thuần ARM (32-bit + 64-bit)
        variant.packaging.jniLibs.excludes.addAll(
            listOf("**/x86/**", "**/x86_64/**")
        )
        variant.outputs.forEach { output ->
            val abiName = output.filters
                .find { it.filterType == FilterConfiguration.FilterType.ABI }
                ?.identifier
            val baseCode = output.versionCode.orNull ?: 1
            // Universal APK giữ mã cơ sở * 1000, bản 32-bit +1, bản 64-bit +2
            output.versionCode.set(baseCode * 1000 + (abiCodes[abiName] ?: 0))
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
}
