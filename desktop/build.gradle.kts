import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(17)
    sourceSets {
        val main by getting {
            kotlin.srcDirs(
                "src/main/kotlin",
                "../app/src/main/java/com/tradesketch/estimator/domain/model",
                "../app/src/main/java/com/tradesketch/estimator/domain/calc",
                "../app/src/main/java/com/tradesketch/estimator/utils"
            )
        }
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation("com.google.code.gson:gson:2.10.1")
}

compose.desktop {
    application {
        mainClass = "com.tradesketch.estimator.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "TradeSketchEstimatorDesktop"
            packageVersion = "1.0.0"
            description = "Offline material takeoff estimator for skilled trades"
        }
    }
}
