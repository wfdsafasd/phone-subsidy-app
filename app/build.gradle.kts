plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    // ⚠️ 출시 전 꼭 바꾸세요: 플레이스토어에 한 번 올리면 applicationId는 영원히 못 바꿉니다.
    namespace = "kr.example.subsidycompare"
    compileSdk = 36

    defaultConfig {
        applicationId = "kr.example.subsidycompare"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        // 매일 갱신되는 지원금 JSON 주소 (가이드 4단계에서 본인 GitHub 주소로 교체)
        // 주소를 바꾸기 전까지 앱은 내장된 샘플 데이터만 보여줍니다.
        buildConfigField(
            "String", "DATA_URL",
            "\"https://raw.githubusercontent.com/YOUR_GITHUB_ID/YOUR_REPO/main/data/subsidies.json\""
        )
        // 개인정보처리방침 주소 (플레이스토어 필수)
        buildConfigField(
            "String", "PRIVACY_URL",
            "\"https://YOUR_GITHUB_ID.github.io/YOUR_REPO/privacy-policy.html\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation("junit:junit:4.13.2")
}
