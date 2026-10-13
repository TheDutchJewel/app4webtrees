package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.bgghome.webtrees.nativ.ui.LocalAppName
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image
import de.bgghome.webtrees.nativ.res.*

/**
 * Marke am PC: das Icon und die Wortmarke "wtWin" (wt in der Hauptfarbe, der Zusatz in Textfarbe), darunter
 * wahlweise der Claim. Steht im Startbildschirm und im Ueber-Dialog. Das Icon liegt als marke/icon.png bei.
 */
object Marke {
    private var bild: ImageBitmap? = null
    /** Eigenes Symbol eines anderen Programms auf demselben Kern (gedaudit: marke/gedaudit.png); null = wtWin. */
    var programm: String? = null

    fun icon(): ImageBitmap? = bild ?: (programm?.let { Marke::class.java.classLoader?.getResourceAsStream("marke/$it.png") }
        ?: Marke::class.java.classLoader?.getResourceAsStream("marke/icon.png"))
        ?.use { Image.makeFromEncoded(it.readBytes()).toComposeImageBitmap() }?.also { bild = it }
}

@Composable
/** [vorsatz]: so viele Buchstaben in der Hauptfarbe - "wt" bei wtWin, "ged" bei gedaudit. */
fun Wortmarke(appName: String = LocalAppName.current, groesse: Int = 32, vorsatz: Int = vorsatzLaenge(appName)) {
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(appName.take(vorsatz)) }
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) { append(appName.drop(vorsatz)) }
    }
    Text(text, fontSize = groesse.sp, lineHeight = (groesse * 1.1f).sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.03).em(groesse))
}

private fun Double.em(groesse: Int) = (this * groesse).sp

fun vorsatzLaenge(appName: String) = if (appName.startsWith("ged")) 3 else 2

@Composable
fun Markenkopf(appName: String = LocalAppName.current, mitClaim: Boolean = true, groesse: Int = 32) {
    val icon = remember { Marke.icon() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((groesse / 2).dp)) {
            icon?.let {
                Image(it, contentDescription = null, modifier = Modifier.size((groesse * 1.9f).dp).clip(RoundedCornerShape((groesse * 0.42f).dp)))
            }
            Wortmarke(appName, groesse)
        }
        if (mitClaim) {
            Text(stringResource(Res.string.desk_claim), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
