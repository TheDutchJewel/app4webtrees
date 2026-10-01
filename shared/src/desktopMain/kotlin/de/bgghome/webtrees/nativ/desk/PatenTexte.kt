package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.Texte
import de.bgghome.webtrees.nativ.api.FactJson
import de.bgghome.webtrees.nativ.data.Heiratsart
import de.bgghome.webtrees.nativ.data.PatenRolle
import de.bgghome.webtrees.nativ.data.heiratsart
import de.bgghome.webtrees.nativ.data.patenZeilen
import de.bgghome.webtrees.nativ.res.*

/* Paten und Heiratsart als Text fuer Druck, Karteikarten, Listen und Buecher (ohne Composable, ueber Texte.t). */

internal fun patenRolleText(r: PatenRolle): String = Texte.t(
    when (r) { PatenRolle.Paten -> Res.string.assoc_godparents; PatenRolle.Trauzeugen -> Res.string.assoc_witnesses; PatenRolle.Beteiligte -> Res.string.assoc_others }
)

internal fun heiratsartKurz(art: Heiratsart): String = Texte.t(
    when (art) { Heiratsart.Standesamtlich -> Res.string.marr_civil; Heiratsart.Kirchlich -> Res.string.marr_religious; Heiratsart.Partnerschaft -> Res.string.marr_partners; Heiratsart.OhneTrauschein -> Res.string.marr_common }
)

/** "Paten: ...", "Trauzeugen: ..." - je Rolle eine Zeile; leer bei Modulen vor API-Stufe 19. */
internal fun patenZeilenText(f: FactJson, trenner: String = "; "): List<String> = f.patenZeilen(Texte.t(Res.string.person_private), ::patenRolleText, trenner)

/** " (standesamtlich)" hinter einer Heirat, leer ohne bekannte Art. */
internal fun heiratsartKlammer(f: FactJson): String = f.heiratsart?.let { " (" + heiratsartKurz(it) + ")" }.orEmpty()
