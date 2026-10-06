package de.bgghome.webtrees.nativ.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Eigene Symbole fuer die Navigationsleiste. Die Standardsammlung von Compose hat weder Baum noch Bild; das
// Teilen-Symbol als Baum und der Kopf im Kreis als Fotos waren Notloesungen.

/** Ein kleiner Stammbaum: eine Karte oben, zwei darunter, durch Linien verbunden. */
val TreeIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Tree", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            path(fill = SolidColor(Color.Black)) {
                // obere Karte
                moveTo(8f, 3f); lineTo(16f, 3f); lineTo(16f, 8f); lineTo(8f, 8f); close()
                // Stamm und Querbalken
                moveTo(11f, 8f); lineTo(13f, 8f); lineTo(13f, 12f); lineTo(11f, 12f); close()
                moveTo(5f, 11f); lineTo(19f, 11f); lineTo(19f, 13f); lineTo(5f, 13f); close()
                // zwei Beine
                moveTo(5f, 12f); lineTo(7f, 12f); lineTo(7f, 16f); lineTo(5f, 16f); close()
                moveTo(17f, 12f); lineTo(19f, 12f); lineTo(19f, 16f); lineTo(17f, 16f); close()
                // untere Karten
                moveTo(2f, 16f); lineTo(10f, 16f); lineTo(10f, 21f); lineTo(2f, 21f); close()
                moveTo(14f, 16f); lineTo(22f, 16f); lineTo(22f, 21f); lineTo(14f, 21f); close()
            }
        }
        .build()
}

/** Ein Bild: Rahmen mit Berg und Sonne (Umriss nach dem Material-Symbol "image"). */
val PhotoIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Photo", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21f, 19f); verticalLineTo(5f)
                curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f); horizontalLineTo(5f)
                curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f); verticalLineTo(19f)
                curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f); horizontalLineTo(19f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f); close()
                moveTo(8.5f, 13.5f); lineTo(11f, 16.51f); lineTo(14.5f, 12f); lineTo(19f, 18f); horizontalLineTo(5f); close()
            }
        }
        .build()
}

/** Dichtes Raster: neun kleine Quadrate. */
val GridIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Grid", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            path(fill = SolidColor(Color.Black)) {
                for (row in 0..2) for (col in 0..2) {
                    val x = 3f + col * 7f; val y = 3f + row * 7f
                    moveTo(x, y); lineTo(x + 5f, y); lineTo(x + 5f, y + 5f); lineTo(x, y + 5f); close()
                }
            }
        }
        .build()
}

/** Symbol aus einem SVG-Pfad (Material-Symbole, Apache 2.0), 24er-Raster. */
private fun pfadSymbol(name: String, pfad: String): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(PathParser().parsePathString(pfad).toNodes(), fill = SolidColor(Color.Black))
        .build()

/** Fragezeichen im Kreis fuer die Hilfe - das "i" ist fuer "Ueber" und Quellen schon vergeben (Material "help_outline"). */
val HelpIcon: ImageVector by lazy {
    pfadSymbol("Help", "M11 18h2v-2h-2v2zm1-16C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm0-14c-2.21 0-4 1.79-4 4h2c0-1.1.9-2 2-2s2 .9 2 2c0 2-3 1.75-3 5h2c0-2.25 3-2.5 3-5 0-2.21-1.79-4-4-4z")
}

/** Zwei Linien, die zusammenlaufen - Personen zusammenfuehren (Material "call_merge"). */
val MergeIcon: ImageVector by lazy {
    pfadSymbol("Merge", "M17 20.41 18.41 19 15 15.59 13.59 17 17 20.41zM7.5 8H11v5.59L5.59 19 7 20.41l6-6V8h3.5L12 3.5 7.5 8z")
}

/** Liste mit Haken fuer Forschungsaufgaben (Material "assignment_turned_in"). */
val TaskIcon: ImageVector by lazy {
    pfadSymbol("Task", "M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm-2 14-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z")
}

/** Weltkugel fuer die Sprache (Material "language"). */
val LanguageIcon: ImageVector by lazy {
    pfadSymbol("Language", "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95c-.32-1.25-.78-2.45-1.38-3.56 1.84.63 3.37 1.91 4.33 3.56zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2s.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56-1.84-.63-3.37-1.9-4.33-3.56zm2.95-8H5.08c.96-1.66 2.49-2.93 4.33-3.56C8.81 5.55 8.35 6.75 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2s.07-1.35.16-2h4.68c.09.65.16 1.32.16 2s-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95c-.96 1.65-2.49 2.93-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2s-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z")
}

/** Uhr mit Rueckwaertspfeil fuer die letzten Aenderungen (Material "history"). */
val HistoryIcon: ImageVector by lazy {
    pfadSymbol("History", "M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z")
}

/** Ein Schriftstueck mit Textzeilen fuer die Quellen (Material "description"). */
val SourceIcon: ImageVector by lazy {
    pfadSymbol("Source", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z")
}
