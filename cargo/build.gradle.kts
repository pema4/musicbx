plugins {
    base
}

project.buildDir = file("target")

tasks.build {
    doFirst {
        exec {
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
    }
}

tasks.clean {
    doFirst {
        exec {
            commandLine("cargo", "clean")
        }
    }
}
