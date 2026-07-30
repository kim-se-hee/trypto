import java.util.Properties

plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// 릴리스 서명 자격증명. `android/key.properties` 는 .gitignore 에 있어 저장소에 들어가지 않는다.
// 키스토어를 잃으면 이미 설치된 앱에 업데이트를 올릴 수 없다 — 파일과 비밀번호를 따로 백업한다.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("key.properties")
    if (file.exists()) file.inputStream().use { load(it) }
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
    }

    signingConfigs {
        create("release") {
            // key.properties 가 없으면 여기서 멈춘다. 디버그 키로 조용히 폴백하지 않는다 —
            // 그렇게 서명한 APK 를 배포하면 나중에 진짜 키로 바꿀 때 기존 설치자가 전부
            // 삭제·재설치해야 하고, 제공자 콘솔의 지문도 무효가 된다.
            val storePath = keystoreProperties.getProperty("storeFile")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

// 릴리스 빌드에만 걸리는 확인. 디버그 빌드와 `flutter test` 는 key.properties 없이도 돌아간다.
tasks.matching { it.name.contains("Release") && it.name.startsWith("package") }.configureEach {
    doFirst {
        if (!rootProject.file("key.properties").exists()) {
            throw GradleException(
                "android/key.properties 가 없어 릴리스 서명을 할 수 없다. mobile/README.md 의 '릴리스 빌드' 절을 따른다."
            )
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
