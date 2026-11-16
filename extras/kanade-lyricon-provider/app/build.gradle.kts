plugins {
    id("com.android.application")
}

android {
    namespace = "com.hollowden.kanade.lyricon"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hollowden.kanade.lyricon"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            // Xposed 入口和 Provider 依赖有反射/序列化路径，关闭 R8 最稳妥。
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("io.github.proify.lyricon:provider:0.1.70")
    compileOnly("de.robv.android.xposed:api:82")
}
