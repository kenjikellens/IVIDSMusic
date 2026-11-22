import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:data"))
            implementation(project(":core:player"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.palette.ktx)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

compose.resources {
    packageOfResClass = "com.kenjigames.ividsmusic.resources"
}

android {
    namespace = "com.kenjigames.ividsmusic"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.kenjigames.ividsmusic"
        minSdk = 24
        targetSdk = 36
        versionCode = 32
        versionName = "0.2.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "device"
    productFlavors {
        create("mobile") {
            dimension = "device"
            buildConfigField("boolean", "IS_TV", "false")
        }
        create("tv") {
            dimension = "device"
            buildConfigField("boolean", "IS_TV", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs.nio)
    debugImplementation(compose.uiTooling)
}

val syncDesktopNativeTools by tasks.registering(Copy::class) {
    description = "Copies bundled Windows native CLI tools such as yt-dlp.exe into desktop app resources."
    val ytDlp = rootProject.file("yt-dlp.exe")
    if (ytDlp.exists()) {
        from(ytDlp)
    }
    val ffmpeg = rootProject.file("ffmpeg.exe")
    if (ffmpeg.exists()) {
        from(ffmpeg)
    }
    into(layout.projectDirectory.dir("resources/windows"))
}

tasks.configureEach {
    if (name.contains("prepareAppResources", ignoreCase = true) ||
        name == "desktopProcessResources" ||
        name == "createDistributable" ||
        name == "packageExe" ||
        name == "run"
    ) {
        dependsOn(syncDesktopNativeTools)
    }
}

compose.desktop {
    application {
        mainClass = "com.kenjigames.ividsmusic.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.AppImage)
            packageName = "IVIDS Music"
            packageVersion = "0.2.5"
            description = "IVIDS Music â€” High-Fidelity Native Music Player & Downloader"
            vendor = "KenjiGames"
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))

            modules(
                "java.sql",
                "java.naming",
                "jdk.unsupported",
                "java.desktop",
                "java.net.http",
                "jdk.crypto.ec"
            )

            windows {
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
                menuGroup = "IVIDS Music"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "9a4c6e50-4f7c-4f9e-b1c2-8f6d2e1b7a01"
            }
        }
    }
}
