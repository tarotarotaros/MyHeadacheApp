plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.myheadacheapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myheadacheapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // （※ここからがポイント）
        // gradle.properties に定義したキー"MY_APP_ENDPOINT_URL"を読み込む
        val endpointUrl = project.findProperty("MY_APP_ENDPOINT_URL") as? String ?: ""
        // BuildConfig の定数として注入 (型, 定数名, 値)
        buildConfigField("String", "ENDPOINT_URL", "\"${endpointUrl}\"")
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // OkHttpの追加 (最新版は公式リポジトリを参照)
    implementation(libs.okhttp)
    implementation(libs.androidx.constraintlayout.v220)
    implementation(libs.logging.interceptor)

}