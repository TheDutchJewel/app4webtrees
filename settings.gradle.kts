pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "app4webtrees"
// shared/  — geteilter Kern (API, Zustand, Oberflaeche), Android + Desktop-JVM
// app/     — Android-Huelle (Activity, Manifest, Launcher, Signatur)
// desktop/ — Linux/Windows-Huelle (Fenster, Pakete deb/msi/exe)
// gedaudit/ — eigenes Programm auf demselben Kern: GEDCOM-Datei pruefen, ohne webtrees (13.10.2026)
include(":shared")
include(":app")
include(":desktop")
include(":gedaudit")
