package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * Vorschau der Tafel mit Zoom (28.09.2026): Mausrad zoomt um die Stelle unter dem Zeiger, Ziehen verschiebt, ein
 * Klick wechselt zwischen ganzem Blatt und Lesegroesse, Rechtsklick meldet den Punkt auf der Seite. Gezeichnet wird
 * das vorhandene Bild skaliert; die Auflösung passt TafelFenster in Stufen an ([zoomStufe]).
 */

/** Das gerenderte Blatt: Bild, Pixel je Einheit der Seite, Angaben zur Tafel. */
class VorschauBild(val bild: ImageBitmap, val skala: Float, val info: TafelInfo)

/** Aufloesungsstufe zum Zoom: 0 bis 1-fach, 1 bis 2-fach, 2 bis 4-fach ... */
fun zoomStufe(zoom: Float): Int = if (zoom <= 1f) 0 else Math.ceil(Math.log(zoom.toDouble()) / Math.log(2.0)).toInt()

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ZoomVorschau(
    v: VorschauBild, zoom: Float, versatz: Offset, onAendern: (Float, Offset) -> Unit,
    onRechtsklick: (seite: Offset, anker: Offset) -> Unit,
) {
    var groesse by remember { mutableStateOf(IntSize.Zero) }
    val w = groesse.width.toFloat(); val h = groesse.height.toFloat()
    // Seite in ihren Einheiten, eingepasster Massstab (Pixel je Einheit)
    val pw = v.bild.width / v.skala; val ph = v.bild.height / v.skala
    val s0 = if (w > 0f && h > 0f) minOf(w / pw, h / ph) else 1f
    // Hoechstens so weit, dass eine Karte etwa 700 Pixel breit ist; Lesegroesse beim Klick: etwa 180 Pixel
    val karteB = v.info.karten.firstOrNull()?.b ?: (pw / 6f)
    val maxZoom = maxOf(1f, 700f / karteB / s0)
    val klickZoom = (180f / karteB / s0).coerceIn(minOf(2f, maxZoom), maxZoom)

    fun lage(z: Float, vs: Offset): Offset {
        val s = s0 * z
        return Offset((w - pw * s) / 2 + vs.x, (h - ph * s) / 2 + vs.y)
    }
    // Das Blatt bleibt im Bild: kleiner als das Fenster mittig, groesser nicht ueber die Raender hinaus
    fun begrenzt(z: Float, vs: Offset): Offset {
        val s = s0 * z
        fun achse(extra: Float, x: Float) = if (extra <= 0f) 0f else x.coerceIn(-extra / 2, extra / 2)
        return Offset(achse(pw * s - w, vs.x), achse(ph * s - h, vs.y))
    }
    // Zoom so, dass der Punkt unter [c] stehen bleibt
    fun zoomUm(c: Offset, neu: Float) {
        val z = neu.coerceIn(1f, maxZoom)
        val p = (c - lage(zoom, versatz)) / (s0 * zoom)
        val s = s0 * z
        val pos = c - p * s
        onAendern(z, begrenzt(z, Offset(pos.x - (w - pw * s) / 2, pos.y - (h - ph * s) / 2)))
    }

    // Klick oder Ziehen mit der linken Taste: erst ab ein paar Pixeln Bewegung ist es Ziehen
    var start by remember { mutableStateOf<Offset?>(null) }
    var zuletzt by remember { mutableStateOf(Offset.Zero) }
    var gezogen by remember { mutableStateOf(false) }
    val vs = begrenzt(zoom, versatz)
    val pos = lage(zoom, vs)
    Canvas(
        Modifier.fillMaxSize().clipToBounds().onSizeChanged { groesse = it }
            .onPointerEvent(PointerEventType.Scroll) { e ->
                val c = e.changes.first()
                zoomUm(c.position, zoom * 1.2f.pow(-c.scrollDelta.y))
                c.consume()
            }
            .onPointerEvent(PointerEventType.Press) { e ->
                val c = e.changes.first().position
                if (e.buttons.isSecondaryPressed) {
                    onRechtsklick((c - pos) / (s0 * zoom), c)
                } else if (e.buttons.isPrimaryPressed) { start = c; zuletzt = c; gezogen = false }
            }
            .onPointerEvent(PointerEventType.Move) { e ->
                val c = e.changes.first().position
                val st = start ?: return@onPointerEvent
                if (!e.buttons.isPrimaryPressed) { start = null; return@onPointerEvent }
                if (!gezogen && (c - st).getDistance() > 4f) gezogen = true
                if (gezogen) onAendern(zoom, begrenzt(zoom, vs + (c - zuletzt)))
                zuletzt = c
            }
            .onPointerEvent(PointerEventType.Release) { e ->
                val c = e.changes.first().position
                if (start != null && !gezogen) {
                    if (zoom > 1.01f) onAendern(1f, Offset.Zero) else zoomUm(c, klickZoom)
                }
                start = null
            },
    ) {
        drawImage(
            v.bild, srcOffset = IntOffset.Zero, srcSize = IntSize(v.bild.width, v.bild.height),
            dstOffset = IntOffset(pos.x.roundToInt(), pos.y.roundToInt()),
            dstSize = IntSize((pw * s0 * zoom).roundToInt().coerceAtLeast(1), (ph * s0 * zoom).roundToInt().coerceAtLeast(1)),
            filterQuality = FilterQuality.Medium,
        )
    }
}
