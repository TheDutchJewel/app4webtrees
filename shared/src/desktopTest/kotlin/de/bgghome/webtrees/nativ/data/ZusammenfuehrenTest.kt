package de.bgghome.webtrees.nativ.data

import de.bgghome.webtrees.nativ.api.MergeFact
import de.bgghome.webtrees.nativ.pruefung.FaktRef
import de.bgghome.webtrees.nativ.pruefung.PruefErgebnis
import de.bgghome.webtrees.nativ.pruefung.Treffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZusammenfuehrenTest {
    @Test
    fun paarAusFaktenUndText() {
        val mitFakten = Treffer("219", "I1", null, "Karl Offen * 1830 = I9", listOf(FaktRef("I1", "a"), FaktRef("I9", "b")))
        assertEquals(Dublette("I1", "I9", "219", mitFakten.text), paarAus(mitFakten))
        val nurText = Treffer("219", "I1", null, "Karl Offen * 1830 = I9")
        assertEquals("I9", paarAus(nurText)?.geht)
        val unscharf = Treffer("228", "I2", null, "≈ Carl Offen (I7) * 1831, * 1830", listOf(FaktRef("I2", "a")))
        assertEquals("I7", paarAus(unscharf)?.geht)
        assertNull(paarAus(Treffer("219", null, "F1", "ohne Person")))
        assertNull(paarAus(Treffer("219", "I1", null, "ohne zweite Kennung")))
    }

    @Test
    fun dublettenOhneDoppelte() {
        val e = PruefErgebnis(mapOf(
            "219" to listOf(Treffer("219", "I1", null, "A = I9", listOf(FaktRef("I1", "a"), FaktRef("I9", "b")))),
            "228" to listOf(Treffer("228", "I9", null, "≈ A (I1)"), Treffer("228", "I3", null, "≈ B (I4)")),
        ))
        val d = dubletten(e)
        assertEquals(listOf("I1|I9", "I3|I4"), d.map { it.schluessel })
        assertEquals("219", d[0].regel, "der sichere Treffer gewinnt")
        assertEquals(Dublette("I9", "I1", "219", "A = I9"), d[0].umgedreht())
    }

    @Test
    fun vorschlagNimmtVerknuepfungenImmer() {
        val f = listOf(MergeFact("a", keep = true), MergeFact("b", keep = false), MergeFact("c", keep = false, link = true))
        assertEquals(setOf("a", "c"), vorschlag(f))
    }
}
