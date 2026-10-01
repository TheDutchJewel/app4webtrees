package de.bgghome.webtrees.nativ

import de.bgghome.webtrees.nativ.data.Ablage
import de.bgghome.webtrees.nativ.data.Settings
import de.bgghome.webtrees.nativ.res.Res
import de.bgghome.webtrees.nativ.res.action_cancel
import de.bgghome.webtrees.nativ.res.allStringResources
import de.bgghome.webtrees.nativ.res.api_language
import de.bgghome.webtrees.nativ.res.desk_decimal_point
import de.bgghome.webtrees.nativ.pruefung.BAUSTEINE_DE
import de.bgghome.webtrees.nativ.pruefung.PruefTexte
import de.bgghome.webtrees.nativ.pruefung.Regelkatalog
import de.bgghome.webtrees.nativ.pruefung.Voreinstellung
import java.io.File
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Die Sprachwahl: jede Sprache der App hat eine vollstaendige strings.xml, die Wahl schaltet die Texte um,
 * und die Pruefung findet alle ihre Schluessel in den Ressourcen.
 */
class SpracheTest {
    private class Speicher : Ablage {
        val werte = HashMap<String, String>()
        override fun getString(key: String, default: String?) = werte[key] ?: default
        override fun putString(key: String, value: String?) { if (value == null) werte.remove(key) else werte[key] = value }
        override fun getBoolean(key: String, default: Boolean) = werte[key]?.toBoolean() ?: default
        override fun putBoolean(key: String, value: Boolean) { werte[key] = value.toString() }
        override fun alle(): Map<String, String> = werte
        override fun leeren() = werte.clear()
    }

    private val ursprung = Locale.getDefault()

    @AfterTest fun zurueck() { Sprache.setzen(null); Locale.setDefault(ursprung) }

    @Test
    fun umschaltenWirktAufTexte() {
        val s = Settings(Speicher())
        Sprache.start(s)
        val erwartet = mapOf("de" to "Abbrechen", "en" to "Cancel", "fr" to "Annuler", "nl" to "Annuleren", "es" to "Cancelar")
        for ((code, text) in erwartet) {
            Sprache.setzen(code)
            assertEquals(code, Sprache.aktiv)
            assertEquals(code, Locale.getDefault().language)
            assertEquals(text, Texte.t(Res.string.action_cancel), "action_cancel in $code")
            assertEquals(code, Texte.t(Res.string.api_language), "api_language in $code")
            assertTrue(Texte.t(Res.string.desk_decimal_point) in listOf(".", ","))
        }
        assertEquals("es", s.language)
        Sprache.setzen(null)
        assertEquals("", s.language)
        assertEquals(ursprung.language, Locale.getDefault().language)
    }

    @Test
    fun pruefungFindetAlleSchluessel() {
        val namen = Res.allStringResources.keys
        BAUSTEINE_DE.keys.forEach { assertTrue("pruef_$it" in namen, "pruef_$it fehlt") }
        Regelkatalog.alle.forEach { assertTrue("regel_${it.id}" in namen, "regel_${it.id} fehlt") }
        Voreinstellung.alle.forEach { assertTrue("preset_${it.id}" in namen, "preset_${it.id} fehlt") }
        // Deutsch im Motor und in den Ressourcen stimmen ueberein
        Sprache.setzen("de")
        BAUSTEINE_DE.forEach { (k, de) -> assertEquals(de, Texte.t(Res.allStringResources.getValue("pruef_$k")), k) }
        Sprache.setzen("fr")
        val fr = PruefTexte.ausRessourcen("fr")!!
        BAUSTEINE_DE.keys.forEach { assertTrue(!fr(it).startsWith("#"), "pruef_$it ohne Text in fr") }
    }

    /** Jede Sprache hat dieselben Schluessel wie die englische Vorlage (ohne translatable="false"). */
    @Test
    fun alleSprachenVollstaendig() {
        val wurzel = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "shared/src/commonMain/composeResources").isDirectory }
        val ordner = File(wurzel, "shared/src/commonMain/composeResources")
        fun schluessel(datei: File, nurUebersetzbar: Boolean): Set<String> {
            val text = datei.readText()
            return Regex("<(string|plurals|string-array) name=\"([^\"]+)\"([^>]*)>").findAll(text)
                .filter { !nurUebersetzbar || "translatable=\"false\"" !in it.groupValues[3] }
                .map { it.groupValues[2] }.toSet()
        }
        val vorlage = schluessel(File(ordner, "values/strings.xml"), true)
        for (code in Sprache.CODES.filter { it != "en" }) {
            val datei = File(ordner, "values-$code/strings.xml")
            assertTrue(datei.isFile, "values-$code/strings.xml fehlt")
            val eigene = schluessel(datei, false)
            assertEquals(emptySet(), vorlage - eigene, "fehlt in $code")
            assertEquals(emptySet(), eigene - vorlage, "zu viel in $code")
        }
        // Compose-Ressourcen loesen \' nicht auf (anders als Android-R): der Backslash stuende sichtbar im Text.
        for (code in Sprache.CODES) {
            val datei = File(ordner, if (code == "en") "values/strings.xml" else "values-$code/strings.xml")
            assertTrue("\\'" !in datei.readText(), "\\' in $code - Apostroph als ’ schreiben")
        }
    }
}
