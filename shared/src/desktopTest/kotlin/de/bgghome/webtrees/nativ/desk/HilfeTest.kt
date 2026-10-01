package de.bgghome.webtrees.nativ.desk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Die Hilfekapitel: vollstaendig in allen Sprachen der App, Markdown sauber zerlegt, alle inneren Links zeigen auf Kapitel. */
class HilfeTest {

    @Test
    fun alleKapitelInAllenSprachen() {
        for (sprache in de.bgghome.webtrees.nativ.Sprache.CODES) {
            val kapitel = HilfeTexte.laden(sprache)
            assertEquals(HilfeTexte.REIHENFOLGE, kapitel.map { it.id }, "Kapitel fehlen in $sprache")
            kapitel.forEach { k ->
                assertTrue(k.titel != k.id, "Kapitel ${k.id} ($sprache) hat keine HUeberschrift")
                assertTrue(k.bloecke.size > 3, "Kapitel ${k.id} ($sprache) ist leer")
            }
        }
        // Unbekannte Sprache faellt auf Englisch zurueck
        assertEquals("Getting started", HilfeTexte.laden("pt").first().titel)
    }

    @Test
    fun innereLinksZeigenAufKapitel() {
        val ziel = Regex("\\]\\(hilfe:([a-z]+)\\)")
        for (sprache in de.bgghome.webtrees.nativ.Sprache.CODES) for (id in HilfeTexte.REIHENFOLGE) {
            val text = HilfeTest::class.java.classLoader.getResourceAsStream("hilfe/$sprache/$id.md")!!.readBytes().toString(Charsets.UTF_8)
            ziel.findAll(text).forEach { m -> assertTrue(m.groupValues[1] in HilfeTexte.REIHENFOLGE, "$sprache/$id verweist auf unbekanntes Kapitel ${m.groupValues[1]}") }
        }
    }

    @Test
    fun markdownBloecke() {
        val b = markdownBloecke(
            """
            # Titel

            Ein HAbsatz über
            zwei Zeilen mit **fett** und `Strg+F`.

            ## Liste
            - eins
            - zwei
              fortgesetzt
            1. erstens
            2. zweitens

            | Taste | Wirkung |
            | - | - |
            | F1 | Hilfe |

            > HHinweis hier.
            """.trimIndent(),
        )
        assertEquals(listOf("HUeberschrift", "HAbsatz", "HUeberschrift", "HAufzaehlung", "HAufzaehlung", "HTabelle", "HHinweis"), b.map { it::class.simpleName })
        assertEquals("Ein HAbsatz über zwei Zeilen mit **fett** und `Strg+F`.", (b[1] as HAbsatz).text)
        assertEquals(listOf("eins", "zwei fortgesetzt"), (b[3] as HAufzaehlung).punkte)
        assertTrue((b[4] as HAufzaehlung).nummeriert)
        val t = b[5] as HTabelle
        assertEquals(listOf("Taste", "Wirkung"), t.kopf); assertEquals(listOf(listOf("F1", "Hilfe")), t.zeilen)
        assertEquals("HHinweis hier.", (b[6] as HHinweis).text)
    }

    @Test
    fun sucheOhneAuszeichnung() {
        assertEquals("Mit Strg+F und Tafeln fett", ohneAuszeichnung("Mit `Strg+F` und [Tafeln](hilfe:tafeln) **fett**"))
        val k = HilfeTexte.kapitel("x", "# T\n\nDer **Plotter** druckt. Plotter sind breit.\n")
        assertEquals(2, k.treffer("plotter"))
        assertTrue(k.ausschnitt("Plotter").contains("Plotter druckt"))
        assertEquals(0, k.treffer(""))
    }
}
