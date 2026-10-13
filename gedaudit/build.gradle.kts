// gedaudit (13.10.2026): die Plausibilitaetspruefung von wtWin als eigenes Programm fuer GEDCOM-Dateien, ohne
// webtrees. Derselbe Kern (:shared), eigene Huelle; native Pakete via jpackage: deb unter Linux, Windows-Installer
// mit Inno Setup (packageInno, installer/gedaudit.iss), dmg auf GitHub (.github/workflows/gedaudit.yml).
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=androidx.compose.ui.ExperimentalComposeUiApi")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    testImplementation(kotlin("test"))
}

tasks.withType<Test> { useJUnitPlatform() }

// Versionsnummer wie bei wtWin: x.y aus gradle.properties, dritte Stelle die Zahl der Commits (Windows verlangt x.y.z
// und ersetzt nur hoehere Nummern).
val versionName = property("gedaudit.versionName") as String
val desktopBuild = property("gedaudit.desktopBuild") as String
val commitZahl: String = runCatching {
    providers.exec { commandLine("git", "rev-list", "--count", "HEAD"); isIgnoreExitValue = true }
        .standardOutput.asText.get().trim().takeIf { it.toIntOrNull() != null }
}.getOrNull() ?: desktopBuild
val windowsVersion = if (versionName.count { it == '.' } == 1) "$versionName.$commitZahl" else versionName
// macOS laesst keine Hauptnummer 0 zu ("The first number in an app-version cannot be zero"): solange gedaudit 0.x heisst,
// traegt das Bundle 1.0.<Commits>; Dateiname (Workflow) und "Ueber" zeigen weiter 0.x.<Commits>.
val macVersion = if (versionName.startsWith("0.")) "1.0.$commitZahl" else windowsVersion
val osName = System.getProperty("os.name").orEmpty()

compose.desktop {
    application {
        mainClass = "de.bgghome.webtrees.nativ.gedaudit.MainKt"
        jvmArgs += "-Dgedaudit.versionName=$versionName"
        jvmArgs += "-Dgedaudit.build=$commitZahl"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Dmg)
            // NIE mehr aendern, sobald das erste Paket verteilt ist: Installationsordner und Startmenue haengen daran.
            packageName = "gedaudit"
            modules("java.instrument", "java.prefs", "jdk.unsupported", "jdk.localedata")
            if (osName.startsWith("Windows")) modules("jdk.crypto.mscapi")
            packageVersion = versionName
            description = "gedaudit - checks GEDCOM files for contradictions (app4webtrees)"
            vendor = "bgg-home.de"

            linux {
                menuGroup = "Office"
                packageName = "gedaudit"
                appRelease = desktopBuild
                iconFile.set(project.file("icons/app.png"))
            }
            // .ged-Dateien im Dateimanager mit gedaudit oeffnen (Linux-Paket; Windows macht es der Inno-Installer,
            // macOS vorerst nicht - der Mac-Bau ist noch im Versuch)
            if (!osName.startsWith("Mac")) fileAssociation("ged", "GEDCOM file", "application/x-gedcom")
            windows {
                packageVersion = windowsVersion
                iconFile.set(project.file("icons/app.ico"))
            }
            macOS {
                // NIE aendern: macOS erkennt das Programm an dieser Kennung.
                bundleID = "de.bgghome.gedaudit"
                packageVersion = macVersion
                dmgPackageVersion = macVersion
                appCategory = "public.app-category.productivity"
                iconFile.set(project.file("icons/app.icns"))
            }
        }
    }
}

// Windows-Installer mit Inno Setup wie bei wtWin (desktop/build.gradle.kts): Sprachwahl, Installation pro Benutzer.
tasks.register<Exec>("packageInno") {
    group = "compose desktop"
    description = "Windows installer (Inno Setup) from the app image"
    dependsOn("createDistributable")
    onlyIf { osName.startsWith("Windows") }
    val quelle = layout.buildDirectory.dir("compose/binaries/main/app/gedaudit").get().asFile
    val ziel = layout.buildDirectory.dir("compose/binaries/main/inno").get().asFile
    val skript = project.file("installer/gedaudit.iss")
    inputs.dir(quelle); inputs.file(skript); outputs.dir(ziel)
    doFirst {
        val kandidaten = listOfNotNull(
            System.getenv("ISCC"),
            System.getenv("ProgramFiles(x86)")?.let { "$it\\Inno Setup 6\\ISCC.exe" },
            System.getenv("LOCALAPPDATA")?.let { "$it\\Programs\\Inno Setup 6\\ISCC.exe" },
        )
        val iscc = kandidaten.firstOrNull { File(it).isFile } ?: error("ISCC.exe (Inno Setup 6) nicht gefunden. Gesucht: $kandidaten")
        ziel.mkdirs()
        commandLine(iscc, "/Q", "/DVersion=$windowsVersion", "/DQuelle=${quelle.path}", "/DZiel=${ziel.path}", skript.path)
    }
    doLast { ziel.listFiles()?.forEach { println("Installer: ${it.path}") } }
}
