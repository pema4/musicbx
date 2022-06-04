import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.1"
    id("idea")
}

group = "ru.pema4"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://jitpack.io")
}

@OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.desktop.components.splitPane)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("com.github.Dansoftowner:jSystemThemeDetector:3.6")
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.20")

    runtimeOnly("org.slf4j:slf4j-simple:1.7.29")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
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

            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
        }
    }
}

val buildBackendInRelease = true
val buildBackend = task<Exec>("buildBackend") {
    description = "cargo build --release"
    group = "native backend"

    workingDir = File("../cargo")
    val args = listOfNotNull(
        "cargo",
        "build",
        if (buildBackendInRelease) "--release" else null,
    )
    commandLine(args)
}

val syncBackend = task<Sync>("syncBackend") {
    group = "native backend"
    dependsOn(buildBackend)

    val targetFolder = if (buildBackendInRelease) "release" else "debug"
    from("../cargo/target/$targetFolder/libeditor_backend.dylib")
    into(project.layout.projectDirectory.dir("resources/macos"))
}

val cleanBackend = task<Task>("cleanBackend") {
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

        delete(project.layout.projectDirectory.dir("resources"))
    }
}

val patchUiDesktopLib = task<Exec>("patchUiDesktopLib") {
    description = "Applies the fix from https://github.com/JetBrains/compose-jb/issues/1969#issuecomment-1100604793"
    group = "native backend"
    dependsOn(tasks.compileKotlin)

    val uiDesktopJarFile = configurations.runtimeClasspath.get().resolve()
        .first { "ui-desktop-1.1.1.jar" in it.absolutePath }
    val classesRootDir = project.layout.buildDirectory.file("classes/kotlin/main").get().asFile

    commandLine(
        "echo",
        "jar",
        "-uf",
        "\"${uiDesktopJarFile.absolutePath}\"",
        "-C",
        "\"$classesRootDir\"",
        "androidx/compose/ui/util/UpdateEffect_desktopKt.class"
    )
}

tasks.clean {
    dependsOn(cleanBackend)
}

tasks.processResources {
    dependsOn(syncBackend)

    val resourcesDir = destinationDir
    doLast {
        copy {
            from(syncBackend.destinationDir)
            into(resourcesDir)
        }
    }
}

project.afterEvaluate {
    tasks.named("prepareAppResources") {
        dependsOn(syncBackend)
    }
}
