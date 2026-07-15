plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.trypto.mobile"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.trypto.mobile"
        minSdk = flutter.minSdkVersion
        targetSdk = 35
        versionCode = flutter.versionCode
        versionName = flutter.versionName

        // 구글 브라우저 인가 코드 흐름의 콜백 스킴(단일 출처). lib/core/env.dart 의 기본값과 같아야 한다.
        // 아직 콘솔 미설정이라 자리만 잡아 둔 값이다(역방향 클라이언트 ID 스킴으로 바뀔 수 있다).
        // 카카오는 SDK 로 전환해 커스텀 스킴 placeholder 가 필요 없다(AndroidManifest 의 AuthCodeHandlerActivity 참조).
        manifestPlaceholders["authCallbackScheme"] = "trypto"
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
