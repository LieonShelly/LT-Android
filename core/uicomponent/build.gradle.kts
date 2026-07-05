plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.littlethingsandroidai.core.uicomponent"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.material)
    implementation(platform(libs.androidx.compose.bom))
}
