package de.bgghome.webtrees.nativ

import de.bgghome.webtrees.nativ.data.Ablage
import de.bgghome.webtrees.nativ.data.Laender
import de.bgghome.webtrees.nativ.data.Ortsanzeige
import de.bgghome.webtrees.nativ.data.Settings
import kotlin.test.Test
import kotlin.test.assertEquals

class OrtsanzeigeTest {
    private class Speicher : Ablage {
        private val m = mutableMapOf<String, Any?>()
        override fun getString(key: String, default: String?) = m[key] as? String ?: default
        override fun putString(key: String, value: String?) { m[key] = value }
        override fun getBoolean(key: String, default: Boolean) = m[key] as? Boolean ?: default
        override fun putBoolean(key: String, value: Boolean) { m[key] = value }
        override fun alle() = m.filterValues { it is String }.mapValues { it.value as String }
        override fun leeren() = m.clear()
    }

    @Test fun laender() {
        assertEquals("NL", Laender.iso2("Netherlands")); assertEquals("NL", Laender.iso2("Niederlande")); assertEquals("NL", Laender.iso2("Nederland"))
        assertEquals("NL", Laender.iso2("NLD")); assertEquals("NL", Laender.iso2("nl")); assertEquals("DE", Laender.iso2("Deutschland"))
        assertEquals("US", Laender.iso2("USA")); assertEquals("LU", Laender.iso2("Luxemburg")); assertEquals("LU", Laender.iso2("Luxembourg"))
        assertEquals(null, Laender.iso2("Niedersachsen")); assertEquals("NLD", Laender.iso3("NL")); assertEquals("DEU", Laender.iso3("DE"))
    }

    @Test fun formen() {
        val s = Settings(Speicher())
        Ortsanzeige.start(s)
        val a = "Amsterdam, Noord-Holland, Netherlands"
        assertEquals(a, Ortsanzeige.text(a))
        Ortsanzeige.setzen(Ortsanzeige.Form.OrtLand, "")
        assertEquals("Amsterdam, Netherlands", Ortsanzeige.text(a))
        Ortsanzeige.setzen(Ortsanzeige.Form.OrtIso2, "")
        assertEquals("Amsterdam, NL", Ortsanzeige.text(a))
        assertEquals("Soestdijk, NL", Ortsanzeige.text("Soestdijk, UT, NLD"))
        assertEquals("Celle, Niedersachsen", Ortsanzeige.text("Celle, Niedersachsen"), "kein Land erkannt: letzter Teil bleibt")
        assertEquals("Celle", Ortsanzeige.text("Celle"))
        Ortsanzeige.setzen(Ortsanzeige.Form.OrtIso3, "")
        assertEquals("Amsterdam, NLD", Ortsanzeige.text(a))
        assertEquals("Walferdange, LUX", Ortsanzeige.text("Walferdange, Luxembourg"))
        // Heimatland: nur der Ortsname, auch bei voller Form; Ausland weiter nach Form
        Ortsanzeige.setzen(Ortsanzeige.Form.OrtIso2, "Netherlands")
        assertEquals("NL", s.homeCountry); assertEquals("OrtIso2", s.placeFormat)
        assertEquals("Amsterdam", Ortsanzeige.text(a))
        assertEquals("Walferdange, LU", Ortsanzeige.text("Walferdange, Luxembourg"))
        Ortsanzeige.setzen(Ortsanzeige.Form.Voll, "nl")
        assertEquals("Amsterdam", Ortsanzeige.text(a))
        assertEquals("Walferdange, Luxembourg", Ortsanzeige.text("Walferdange, Luxembourg"))
        // Gespeichert und beim naechsten Start wieder da
        Ortsanzeige.start(s)
        assertEquals(Ortsanzeige.Form.Voll, Ortsanzeige.form.value); assertEquals("NL", Ortsanzeige.heimat.value)
        Ortsanzeige.setzen(Ortsanzeige.Form.Voll, "")
        assertEquals("", Ortsanzeige.text(null)); assertEquals("", Ortsanzeige.text(""))
    }
}
