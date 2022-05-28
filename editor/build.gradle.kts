import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.1"
}

group = "ru.pema4"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://jitpack.io")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.jetbrains.compose.components:components-splitpane-desktop:1.1.1")

    implementation("com.github.Dansoftowner:jSystemThemeDetector:3.6")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.freeCompilerArgs = listOf(
        "-opt-in=kotlin.RequiresOptIn",
    )
}

compose.desktop {
    application {
        mainClass = "ru.pema4.musicbx.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "editor"
            packageVersion = "1.0.0"
        }
    }
}

task<DefaultTask>("buildBackend") {
    description = "cargo build --release"
    group = "native backend"

    doLast {
        exec {
            workingDir = File("../cargo")
            commandLine(
                "cargo",
                "build",
                "--release",
            )
        }
    }

    shouldRunAfter("build")
}

tasks.jar {
    dependsOn("buildBackend")

    doLast {
        copy {
            from("../cargo/target/release/libeditor_backend.dylib")
            into("build/resources/main")
        }
    }
}

task<DefaultTask>("cleanBackend") {
    description = "cargo clean"
    group = "native backend"

    doFirst {
        exec {
            workingDir = File("../cargo")
            commandLine(
                "cargo",
                "clean",
            )
        }
    }
}

tasks.clean {
    dependsOn("cleanBackend")
}
