import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
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

dependencies {
    implementation(project(":cargo"))

    implementation(compose.desktop.currentOs)
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    implementation(compose.desktop.components.splitPane)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    implementation("com.github.Dansoftowner:jSystemThemeDetector:3.8")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.2")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    runtimeOnly("org.slf4j:slf4j-simple:2.0.3")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
        excludeDirs = setOf(file("resources"))
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.freeCompilerArgs = listOf(
        "-opt-in=kotlin.RequiresOptIn"
    )
}

compose.desktop {
    application {
        mainClass = "ru.pema4.musicbx.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "musicbx"
            packageVersion = "1.0.0"

            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
        }
    }
}

val syncBackend = task<Sync>("syncBackend") {
    group = "native backend"
    dependsOn(":cargo:build")

    val buildBackendInRelease = rootProject
        .extra["cargo.build_backend_in_release"]
        .toString()
        .toBoolean()

    val targetFolder = if (buildBackendInRelease) "release" else "debug"
    from("../cargo/target/$targetFolder/libeditor_backend.dylib")
    into(project.layout.projectDirectory.dir("resources/macos"))
}

val patchUiDesktopLib = task<Exec>("patchUiDesktopLib") {
    description = "Applies the fix from https://github.com/JetBrains/compose-jb/issues/1969#issuecomment-1100604793"
    group = "native backend"
    dependsOn(tasks.compileKotlin)

    val uiDesktopJarFile = configurations.runtimeClasspath.get().resolve()
        .firstOrNull { "ui-desktop-1.1.1.jar" in it.absolutePath }
        ?: return@task
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
    delete("resources")
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
