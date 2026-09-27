// Linux/Windows/macOS-Huelle von wtAnd: ein Fenster um den geteilten Kern
// (:shared), native Pakete via jpackage. Gebaut wird je System, auf dem
// es laeuft: deb hier, msi/exe auf dem Windows-Laptop (WiX noetig), dmg
// auf GitHub (.github/workflows/mac.yml, noch unsigniert).
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
}

// wtAnd zaehlt zweistellig (1.18); Windows-Installer verlangen x.y.z. Die dritte Stelle zaehlt jetzt von selbst:
// die Zahl der Commits auf dem Stand, der gebaut wird. Windows Installer ersetzt eine installierte Fassung nur
// durch eine mit hoeherer Nummer - bei gleicher Nummer passiert auf Doppelklick schlicht nichts (24./25.09.2026,
// erst mit fester 0, dann mit dem Handzaehler desktopBuild, den vor dem Bauen niemand erhoeht hat). Ohne Git
// (z. B. Quelltext-Archiv) bleibt es beim Handzaehler.
val versionName = property("wtand.versionName") as String
val desktopBuild = property("wtand.desktopBuild") as String
val commitZahl: String = runCatching {
    providers.exec { commandLine("git", "rev-list", "--count", "HEAD"); isIgnoreExitValue = true }
        .standardOutput.asText.get().trim().takeIf { it.toIntOrNull() != null }
}.getOrNull() ?: desktopBuild
val windowsVersion = if (versionName.count { it == '.' } == 1) "$versionName.$commitZahl" else versionName

// Ein Code, zwei Namen: wtWin unter Windows, wtTux unter Linux. Jedes Paket wird auf
// seinem eigenen System gebaut, darum entscheidet das System, auf dem Gradle laeuft.
// wtMac kam am 27.09.2026 dazu (versuchsweise, gebaut nur auf GitHub).
val osName = System.getProperty("os.name").orEmpty()
val appName = when {
    osName.startsWith("Windows") -> "wtWin"
    osName.startsWith("Mac") -> "wtMac"
    else -> "wtTux"
}

compose.desktop {
    application {
        mainClass = "de.bgghome.webtrees.nativ.desktop.MainKt"
        jvmArgs += "-Dwtand.desktopBuild=${property("wtand.desktopBuild")}"
        jvmArgs += "-Dwtand.versionName=$versionName"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg)
            // NIE mehr aendern, sobald das erste Paket verteilt ist: Installationsordner und
            // Startmenue haengen daran (wie upgradeUuid unten).
            packageName = appName
            modules("java.instrument", "java.prefs", "jdk.unsupported")
            // Dieselbe Nummer wie die APK (gradle.properties). Windows
            // Installer verlangt rein numerisch x.y.z.
            packageVersion = versionName
            description = "$appName - client for webtrees (app4webtrees)"
            vendor = "bgg-home.de"

            linux {
                menuGroup = "Office"
                packageName = "wttux"
                appRelease = property("wtand.desktopBuild") as String
                // Dasselbe Symbol wie wtAnd; ohne Angabe zeigt das Menue das Java-Standardsymbol.
                iconFile.set(project.file("icons/app.png"))
            }
            windows {
                packageVersion = windowsVersion
                perUserInstall = true
                menuGroup = "wtWin"
                shortcut = true
                dirChooser = true
                // NIE aendern: nur mit gleicher Kennung ersetzt eine neue
                // Version die alte, statt zweimal im Startmenue zu stehen.
                upgradeUuid = "b3f1d7a2-4c58-4e9b-8f60-1d2e7a9c5b41"
                iconFile.set(project.file("icons/app.ico"))
            }
            macOS {
                // NIE aendern (wie upgradeUuid): macOS erkennt das Programm an dieser Kennung.
                bundleID = "de.bgghome.webtrees.wtmac"
                packageVersion = windowsVersion
                dmgPackageVersion = windowsVersion
                appCategory = "public.app-category.productivity"
                iconFile.set(project.file("icons/app.icns"))
                // Verbinden-Links: wtmac:// und die Schemata der Geschwister, denn api4webtrees waehlt nach
                // Geraet und kennt den Mac (noch) nicht. Die Links kommen als Apple-Event, nicht als Argument (Main.kt).
                infoPlist {
                    extraKeysRawXml = """
                        <key>CFBundleURLTypes</key>
                        <array>
                          <dict>
                            <key>CFBundleURLName</key>
                            <string>de.bgghome.webtrees.connect</string>
                            <key>CFBundleURLSchemes</key>
                            <array>
                              <string>wtmac</string>
                              <string>wtwin</string>
                              <string>wttux</string>
                            </array>
                          </dict>
                        </array>
                    """.trimIndent()
                }
            }
        }
    }
}
