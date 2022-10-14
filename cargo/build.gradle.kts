plugins {
    base
}

project.buildDir = file("target")

val buildBackend = task<Exec>("buildBackend") {
    description = "cargo build --release"
    group = "native backend"

    val buildBackendInRelease = rootProject
        .extra["cargo.build_backend_in_release"]
        .toString()
        .toBoolean()

    workingDir = File("../cargo")
    val args = listOfNotNull(
        "cargo",
        "build",
        if (buildBackendInRelease) "--release" else null
    )
    commandLine(args)
}

tasks.build {
    dependsOn(buildBackend)
}

val cleanBackend = task<Task>("cleanBackend") {
    description = "cargo clean"
    group = "native backend"

    doFirst {
        exec {
            commandLine(
                "cargo",
                "clean"
            )
        }
    }
}
// tasks.clean {
//     dependsOn(cleanBackend)
// }
