plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.edata"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.edata"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // ===== Apache POI（关键）=====
    // 基础包：提供 org.apache.poi.ss.*（否则会报 Unresolved reference 'ss'）
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // 轻量版 OOXML：用于 .xlsx（与 poi 版本保持一致）
    implementation("org.apache.poi:poi-ooxml-lite:5.2.5")

    configurations.all {
        exclude(group = "org.apache.logging.log4j")
    }

    // 如遇到运行期/编译期提示缺少 xmlbeans（一般 ooxml-lite 不需要），再按需补上：
    // implementation("org.apache.xmlbeans:xmlbeans:5.2.1")

    // ===== 日志实现（Android 友好，替代 Log4j）=====
    implementation("org.slf4j:slf4j-android:1.7.36")
    // （或使用 logback-android：
    // implementation("org.slf4j:slf4j-api:1.7.36")
    // implementation("com.github.tony19:logback-android:3.0.0")
    // 选其一即可）

    // 协程（保留你的原有版本；如需可升级）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}