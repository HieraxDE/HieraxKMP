import org.jetbrains.compose.desktop.application.dsl.TargetFormat



plugins {
//    alias(libs.plugins.kotlinMultiplatform)
//    alias(libs.plugins.composeMultiplatform)
//    alias(libs.plugins.composeCompiler)
//    alias(libs.plugins.composeHotReload)
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.compose") version "1.8.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
}

group = "com.momid"
version = "1.0-SNAPSHOT"

kotlin {
//    jvmToolchain(16)
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)

    implementation("com.github.zahid4kh:deskit:1.2.0")
    implementation(compose.material3)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.components.resources)
    implementation(compose.components.uiToolingPreview)
    api(compose.materialIconsExtended)

//    implementation(libs.androidx.lifecycle.viewmodel)
//    implementation(libs.androidx.lifecycle.runtimeCompose)
//    implementation("androidx.compose.material3:material3-android:1.3.2")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Ide"
            packageVersion = "1.0.0"
        }
    }
}
