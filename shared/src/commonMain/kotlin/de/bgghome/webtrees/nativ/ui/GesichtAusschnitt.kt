package de.bgghome.webtrees.nativ.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.bgghome.webtrees.nativ.api.ArchiveEntry
import de.bgghome.webtrees.nativ.api.Bereich

/**
 * Das Gesicht [b] aus dem Vorschaubild als quadratischer Ausschnitt der Kante [kante]: das ganze Bild so gross zeichnen,
 * dass der Rahmen die Kante fuellt, und so verschieben, dass er oben links liegt. Ohne Rahmen das ganze Bild.
 */
@Composable
fun GesichtAusschnitt(e: ArchiveEntry, b: Bereich?, kante: Dp) {
    val bild = e.kachel ?: e.vollbild ?: e.original
    Box(Modifier.size(kante).clipToBounds().border(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        if (b == null || b.w <= 0.0 || b.h <= 0.0 || e.breite <= 0 || e.hoehe <= 0) {
            AsyncImage(model = bild, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(kante))
        } else {
            // groessere Kante des Rahmens fuellt das Quadrat; das Gesicht steht mittig
            val seitenverhaeltnis = e.hoehe.toDouble() / e.breite
            val rahmenB = b.w
            val rahmenH = b.h * seitenverhaeltnis
            val massstab = 1.0 / maxOf(rahmenB, rahmenH)
            val breite = kante * massstab.toFloat()
            val hoehe = breite * seitenverhaeltnis.toFloat()
            val dx = (rahmenB - maxOf(rahmenB, rahmenH)) / 2
            val dy = (rahmenH - maxOf(rahmenB, rahmenH)) / 2
            AsyncImage(
                model = bild, contentDescription = null, contentScale = ContentScale.FillBounds,
                modifier = Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(breite, hoehe)
                    .offset(-(breite * (b.x + dx).toFloat()), -(hoehe * ((b.y * seitenverhaeltnis + dy) / seitenverhaeltnis).toFloat())),
            )
        }
    }
}
