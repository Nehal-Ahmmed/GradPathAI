import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(project(":app:shared"))
    implementation(libs.kotlinx.serialization.json)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "org.gradpath.ai.MainKt"
        jvmArgs += listOf(
            "-Dskiko.renderApi=SOFTWARE",
            "-Dfile.encoding=UTF-8"
        )

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "GradPathAI"
            packageVersion = "1.0.0"
            description = "GradPath AI - Graduate Research & Admissions Partner"
            copyright = "© 2026 GradPath AI"
            vendor = "GradPath AI"

            windows {
                menuGroup = "GradPath AI"
                upgradeUuid = "6d7249b6-8968-4521-8869-7bc285121111"
                dirChooser = true
                perUserInstall = true
                shortcut = true
            }
        }
    }
}