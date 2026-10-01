package de.bgghome.webtrees.nativ.data

import de.bgghome.webtrees.nativ.api.Associate
import de.bgghome.webtrees.nativ.api.FactJson
import de.bgghome.webtrees.nativ.api.FreeAssociate
import de.bgghome.webtrees.nativ.api.SourceRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PatenTest {
    private val louise = Associate("I62", "Louise Falkenrath", "F", "godparent", "godparent", "Patin")
    private val ernst = Associate("I65", "Ernst Falkenrath", "M", "godparent", "godparent", "Pate", notes = listOf("in Abwesenheit"), sources = listOf(SourceRef("S81", "KB Celle")))
    private val plate = FreeAssociate("godparent", null, null, "Friedrich Plate, Anbauer zu Celle")
    private val taufe = FactJson(
        "f1", "CHR", "Taufe", notes = listOf("Paten: Friedrich Plate, Anbauer zu Celle", "Nottaufe"), noteKinds = listOf("associates", "note"),
        associates = listOf(louise, ernst), freeAssociates = listOf(plate),
    )

    @Test
    fun gruppenVerlinktVorFrei() {
        val g = taufe.patenGruppen()
        assertEquals(1, g.size)
        assertEquals(PatenRolle.Paten, g[0].rolle)
        assertEquals(listOf("I62", "I65", null), g[0].eintraege.map { it.xref })
        assertEquals("Louise Falkenrath (Patin); Ernst Falkenrath (Pate); Friedrich Plate, Anbauer zu Celle", taufe.patenText("privat"))
        assertEquals(listOf("Paten: Louise Falkenrath (Patin); Ernst Falkenrath (Pate); Friedrich Plate, Anbauer zu Celle"), taufe.patenZeilen("privat", { it.name }))
        assertTrue(g[0].eintraege[1].notes.isNotEmpty() && g[0].eintraege[1].sources.isNotEmpty())
    }

    @Test
    fun notizMitPatenNichtDoppelt() {
        assertEquals(listOf("Nottaufe"), taufe.notizenOhnePaten())
        // aelteres Modul ohne noteKinds: alles bleibt, die Notiz ist der einzige Ort der Paten
        val alt = FactJson("f2", "CHR", notes = listOf("Paten: Heinrich Eggers; Johann Lüders"))
        assertEquals(alt.notes, alt.notizenOhnePaten())
        assertEquals("Heinrich Eggers; Johann Lüders", alt.patenNotiz())
        assertEquals("Heinrich Eggers; Johann Lüders", alt.patenTextOderNotiz("privat"))
        assertEquals("", alt.patenText("privat"))
        assertNull(FactJson("f3", "CHR", notes = listOf("Nottaufe")).patenNotiz())
    }

    @Test
    fun privatOhneNamen() {
        val p = Associate("I10", null, null, "godparent", "godparent", "", isPrivate = true)
        val f = FactJson("f", "CHR", associates = listOf(p))
        val e = f.patenGruppen().single().eintraege.single()
        assertEquals("privat", e.anzeige("privat"))
        assertEquals(false, e.verlinkt)
        assertEquals("privat", f.patenText("privat"))
    }

    @Test
    fun rollenGetrennt() {
        val f = FactJson(
            "f", "MARR", associates = listOf(Associate("I79", "Ludwig Rosskamp", "M", "witness", "witness", "Zeuge"), Associate("I2", "X Y", "M", "other", "other", "Nachbar")),
            freeAssociates = listOf(FreeAssociate("witness", "Wilhelm Niebuhr", "Lokomotivführer", "Wilhelm Niebuhr, Lokomotivführer")),
        )
        assertEquals(listOf(PatenRolle.Trauzeugen, PatenRolle.Beteiligte), f.patenGruppen().map { it.rolle })
        assertEquals(listOf("Ludwig Rosskamp (Zeuge)", "Wilhelm Niebuhr, Lokomotivführer"), f.patenGruppen()[0].eintraege.map { it.anzeige("privat") })
    }

    @Test
    fun assoDoppelungAusblenden() {
        val level1 = Associate("I350", "Louise Eggers", "F", "Godmother", "godparent", "Patin", level1 = true)
        val chr = FactJson("c", "CHR", associates = listOf(level1))
        val asso = FactJson("a", "ASSO", "Verbundene Person", value = "Louise Eggers", associates = listOf(level1))
        val fremd = FactJson("b", "ASSO", "Verbundene Person", value = "Nachbar", associates = listOf(Associate("I9", "Nachbar", "M", "Nachbar", "other", "")))
        assertEquals(listOf("c", "b"), listOf(chr, asso, fremd).ohneDoppelteAsso().map { it.id })
        // ohne Taufe mit level1-Paten bleibt alles stehen (auch bei aelteren Modulen ohne associates)
        assertEquals(listOf("a", "b"), listOf(asso, fremd).ohneDoppelteAsso().map { it.id })
        assertEquals(1, listOf(FactJson("x", "ASSO")).ohneDoppelteAsso().size)
    }

    @Test
    fun heiratsart() {
        assertEquals(Heiratsart.Standesamtlich, Heiratsart.aus("CIVIL"))
        assertEquals(Heiratsart.Standesamtlich, Heiratsart.aus("civil"))
        assertEquals(Heiratsart.Kirchlich, Heiratsart.aus("Religious"))
        assertEquals(Heiratsart.Partnerschaft, Heiratsart.aus("PARTNERS"))
        assertEquals(Heiratsart.OhneTrauschein, Heiratsart.aus("COMMON LAW"))
        assertNull(Heiratsart.aus(""))
        assertNull(Heiratsart.aus("Hochzeit"))
        val fb: (Heiratsart) -> String = { if (it == Heiratsart.Standesamtlich) "standesamtlich" else "kirchlich" }
        // Stufe 19: Label enthaelt die Art schon -> kein Zusatz
        assertNull(FactJson("1", "MARR", "Standesamtliche Heirat", type = "CIVIL", typeLabel = "Standesamtliche Heirat").artZusatz(fb))
        assertNull(FactJson("1", "MARR", "Civil marriage", type = "CIVIL", typeLabel = "Civil marriage").artZusatz(fb))
        // aelteres Modul: Uebersetzung aus der App, aber nicht doppelt
        assertNull(FactJson("1", "MARR", "Kirchliche Trauung", type = "RELIGIOUS").artZusatz(fb))
        assertEquals("standesamtlich", FactJson("1", "MARR", "Heirat", type = "civil").artZusatz(fb))
        // andere Fakten: Rohwert bzw. typeLabel
        assertEquals("Rufname", FactJson("1", "NAME", "Name", type = "Rufname").artZusatz(fb))
        assertEquals("Spitzname", FactJson("1", "NAME", "Name", type = "aka", typeLabel = "Spitzname").artZusatz(fb))
        assertNull(FactJson("1", "MARR", "Heirat").artZusatz(fb))
    }

    @Test
    fun hauptHeiratStandesamtlichZuerst() {
        val kirch = FactJson("k", "MARR", type = "RELIGIOUS"); val stand = FactJson("s", "MARR", type = "CIVIL"); val ohne = FactJson("o", "MARR")
        assertEquals("s", listOf(kirch, stand).hauptHeirat()?.id)
        assertEquals("k", listOf(kirch, ohne).hauptHeirat()?.id)
        assertNull(listOf(FactJson("d", "DIV")).hauptHeirat())
    }
}
