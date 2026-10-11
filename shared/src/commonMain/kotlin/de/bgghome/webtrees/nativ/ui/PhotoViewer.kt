package de.bgghome.webtrees.nativ.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.api.ArchiveEntry
import de.bgghome.webtrees.nativ.api.ExifRequest

/**
 * Vollbild-Betrachter: wischen zum naechsten Bild, kneifen zum Vergroessern (bis 5x), ein Finger schiebt das vergroesserte
 * Bild, Doppeltipp springt an die Stelle heran und wieder zurueck, ein Tipp blendet Kopf- und Fusszeile aus. Gewischt
 * wird nur, solange nicht vergroessert ist - sonst wuerde jedes Schieben das Bild wechseln.
 *
 * Kurz vor dem Ende meldet onIndex die Stelle; das View-Model laedt dann die naechste Seite nach, und die Liste waechst.
 */
@Composable
fun PhotoViewer(
    viewer: ViewerState,
    onIndex: (Int) -> Unit,
    onClose: () -> Unit,
    onOpenWeb: (String) -> Unit,
    /** Beschriftung aendern: ob dieses Bild beschriftbar ist, der Eintrag in Arbeit, und die drei Handgriffe dazu */
    canEdit: (ViewerItem) -> Boolean = { false },
    editing: ArchiveEntry? = null,
    suggestPersons: PlaceSuggest? = null,
    onEdit: (ViewerItem) -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onSaveExif: (ArchiveEntry, ExifRequest) -> Unit = { _, _ -> },
) {
    val pagerState = rememberPagerState(initialPage = viewer.index) { viewer.items.size }
    var chrome by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) { onIndex(pagerState.currentPage) }

    val item = viewer.items.getOrNull(pagerState.currentPage)
    // Drehen zum Lesen (quer beschriebene Postkarten), je Bild in Vierteldrehungen - reine Ansicht, nichts wird gespeichert
    val drehung = remember(viewer.items) { mutableStateMapOf<Int, Int>() }
    fun drehen(um: Int) { val p = pagerState.currentPage; drehung[p] = ((drehung[p] ?: 0) + um + 4) % 4 }
    val scope = rememberCoroutineScope()
    val fokus = remember { FocusRequester() }
    fun gehe(i: Int) { if (i in viewer.items.indices) scope.launch { pagerState.animateScrollToPage(i) } }
    // Tastatur (PC, Handy mit Tastatur): Pfeile blaettern, Pos1/Ende springen - wie in der Lightbox des Moduls
    LaunchedEffect(Unit) { runCatching { fokus.requestFocus() } }

    Box(Modifier.fillMaxSize().background(Color.Black).focusRequester(fokus).focusable()
        .onPreviewKeyEvent { e ->
            if (e.type != KeyEventType.KeyDown || zoomed) return@onPreviewKeyEvent false
            when (e.key) {
                Key.DirectionRight -> { gehe(pagerState.currentPage + 1); true }
                Key.DirectionLeft -> { gehe(pagerState.currentPage - 1); true }
                Key.MoveHome -> { gehe(0); true }
                Key.MoveEnd -> { gehe(viewer.items.lastIndex); true }
                Key.R -> { drehen(if (e.isShiftPressed) -1 else 1); true }
                else -> false
            }
        }) {
        HorizontalPager(state = pagerState, userScrollEnabled = !zoomed, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { page ->
            ZoomableImage(viewer.items[page], drehung = drehung[page] ?: 0, onTap = { chrome = !chrome }, onZoom = { if (page == pagerState.currentPage) zoomed = it })
        }

        AnimatedVisibility(chrome, Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).statusBarsPadding().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.action_close), tint = Color.White) }
                Text(
                    stringResource(Res.string.viewer_position, pagerState.currentPage + 1, viewer.items.size),
                    Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = { drehen(1) }) { Text("↻", color = Color.White, style = MaterialTheme.typography.titleLarge) }
                item?.takeIf(canEdit)?.let { editable ->
                    IconButton(onClick = { onEdit(editable) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.viewer_edit_exif), tint = Color.White)
                    }
                }
                item?.webUrl?.let { url ->
                    IconButton(onClick = { onOpenWeb(url) }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = stringResource(Res.string.viewer_open_web), tint = Color.White)
                    }
                }
            }
        }

        editing?.let { entry ->
            ExifEditDialog(entry, suggestPersons, onDismiss = onCancelEdit, onSave = { onSaveExif(entry, it) })
        }

        AnimatedVisibility(chrome && item != null, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding()) {
                // Vorschaustreifen wie in der Lightbox - nur am PC, am Handy wird gewischt
                if (LocalDeskMode.current && viewer.items.size > 1) {
                    val streifen = rememberLazyListState()
                    LaunchedEffect(pagerState.currentPage) { streifen.animateScrollToItem((pagerState.currentPage - 3).coerceAtLeast(0)) }
                    LazyRow(Modifier.fillMaxWidth().padding(bottom = 8.dp), state = streifen, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(viewer.items) { i, it ->
                            AsyncImage(model = it.thumb ?: it.image, contentDescription = it.caption, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).border(if (i == pagerState.currentPage) 2.dp else 0.dp, if (i == pagerState.currentPage) Color.White else Color.Transparent)
                                    .clickable { gehe(i) })
                        }
                    }
                }
                Text(item?.caption.orEmpty(), color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                val link = when (item?.link) {
                    LinkState.ObjectOnly -> stringResource(Res.string.archive_link_object)
                    LinkState.FileOnly -> stringResource(Res.string.archive_link_file)
                    else -> null
                }
                val subtitle = listOfNotNull(item?.subtitle?.takeIf { it.isNotBlank() }, link).joinToString(" · ")
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

/** Ein Bild mit Kneifzoom, Verschieben und Doppeltipp. Zoom und Lage gehoeren zum Bild - beim Wechsel beginnt das naechste bei 1x. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoomableImage(item: ViewerItem, drehung: Int = 0, onTap: () -> Unit, onZoom: (Boolean) -> Unit) {
    var bild by remember(item) { mutableStateOf(androidx.compose.ui.geometry.Size.Unspecified) }
    var scale by remember(item) { mutableFloatStateOf(1f) }
    var offset by remember(item) { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    // Das vergroesserte Bild darf nicht aus dem Rahmen rutschen: so weit, dass sein Rand hoechstens den Rahmen erreicht.
    fun clamp(o: Offset, s: Float): Offset {
        val maxX = (size.width * (s - 1f)) / 2f
        val maxY = (size.height * (s - 1f)) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    fun apply(newScale: Float, newOffset: Offset) {
        scale = newScale.coerceIn(1f, MAX_ZOOM)
        offset = if (scale > 1f) clamp(newOffset, scale) else Offset.Zero
        onZoom(scale > 1f)
    }

    val transform = rememberTransformableState { zoomChange, panChange, _ ->
        apply(scale * zoomChange, offset + panChange)
    }

    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(item) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tap ->
                        if (scale > 1f) {
                            apply(1f, Offset.Zero)
                        } else {
                            // Die angetippte Stelle bleibt unter dem Finger: Verschiebung = Abstand zur Mitte * (1 - Zoom).
                            val centre = Offset(size.width / 2f, size.height / 2f)
                            apply(DOUBLE_TAP_ZOOM, (tap - centre) * (1f - DOUBLE_TAP_ZOOM))
                        }
                    },
                )
            }
            // Ein Finger schiebt nur das vergroesserte Bild; bei 1x geht die Bewegung an den Pager (wischen).
            .transformable(transform, canPan = { scale > 1f }),
        contentAlignment = Alignment.Center,
    ) {
        // Bei einer Vierteldrehung tauschen Breite und Hoehe - so weit verkleinern/vergroessern, dass das Bild wieder hineinpasst
        val passend = if (drehung % 2 == 1 && bild.isSpecified && bild.width > 0 && bild.height > 0 && size.width > 0 && size.height > 0) {
            val s = minOf(size.width / bild.width, size.height / bild.height)
            minOf(size.width / (bild.height * s), size.height / (bild.width * s))
        } else 1f
        AsyncImage(
            model = item.image, contentDescription = item.caption, contentScale = ContentScale.Fit,
            onState = { st -> if (st is coil3.compose.AsyncImagePainter.State.Success) bild = st.painter.intrinsicSize },
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale * passend; scaleY = scale * passend
                rotationZ = 90f * drehung
                translationX = offset.x; translationY = offset.y
            },
        )
    }
}
