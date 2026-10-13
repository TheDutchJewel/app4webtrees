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
// gedaudit (GEDCOM-Datei pruefen, ohne webtrees) hat seit 13.10.2026 ein eigenes Repository thobgg/gedaudit, das dieses Repo als
// Submodul einbindet und :shared mitbaut; sein Kern (gedcom/, desk/GedauditFenster.kt, VergleichFenster.kt, AufbauFenster.kt) liegt hier.
include(":shared")
include(":app")
include(":desktop")
