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

/** Ein Schriftstueck mit Textzeilen fuer die Quellen (Material "description"). */
val SourceIcon: ImageVector by lazy {
    pfadSymbol("Source", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z")
}
