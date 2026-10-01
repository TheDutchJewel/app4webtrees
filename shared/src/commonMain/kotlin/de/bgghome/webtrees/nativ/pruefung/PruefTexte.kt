package de.bgghome.webtrees.nativ.pruefung

import androidx.compose.runtime.Composable
import de.bgghome.webtrees.nativ.Texte
import de.bgghome.webtrees.nativ.res.Res
import de.bgghome.webtrees.nativ.res.allStringResources
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Die Texte der Pruefung aus den Ressourcen: Fragen zu den Regeln (regel_<id>), Namen der Voreinstellungen
 * (preset_<id>) und Bausteine der Begruendungen (pruef_<schluessel>). Der Motor kennt nur die Schluessel.
 */
object PruefTexte {
    private fun res(name: String): StringResource = Res.allStringResources.getValue(name)

    fun frage(r: Regel): String = Texte.t(res("regel_${r.id}"))
    fun name(v: Voreinstellung): String = Texte.t(res("preset_${v.id}"))

    @Composable fun frageText(r: Regel): String = stringResource(res("regel_${r.id}"))
    @Composable fun nameText(v: Voreinstellung): String = stringResource(res("preset_${v.id}"))

    /** Bausteine in der Sprache der Oberflaeche fuer [PruefOptionen.texte]; Deutsch kommt aus dem Motor selbst (null). */
    fun ausRessourcen(sprache: String): ((String) -> String)? =
        if (sprache == "de") null else { schluessel -> Texte.t(res("pruef_$schluessel")) }
}
