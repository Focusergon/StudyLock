pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Espejo local para artefactos grandes que fallan por red inestable
        // (p. ej. material-icons-extended, ~35 MB). Ver local-repo/README.md.
        maven {
            name = "localMirror"
            url = uri(rootDir.resolve("local-repo"))
            metadataSources {
                gradleMetadata()
                mavenPom()
                artifact()
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "StudyLock"
include(":app")
