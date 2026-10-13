package de.bgghome.webtrees.nativ.gedcom

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Zwei Staende derselben Datei: eine Person geaendert, eine entfernt, eine neu, eine mit neuer Kennung und Schreibvariante. */
class GedcomVergleichTest {

    private val falkenrath = File("../testdaten/falkenrath-1.3.ged")

    @Test
    fun staendeVergleichen() {
        // Unter Windows checkt Git die Datei mit CRLF aus, die Muster unten rechnen mit \n
        val text = falkenrath.readText().replace("\r\n", "\n")
        val a = GedcomLeser.alsExport(GedcomLeser.zerlegen(text), jetzt = 2026)
        // B: I130 bekommt ein anderes Sterbedatum, I1 verschwindet, I9999 kommt dazu, I2 bekommt die Kennung I7777 und "Katharina" statt "Catharina"
        var t = text
        val i130 = Regex("(?s)0 @I130@ INDI.*?(?=\n0 @)").find(t)!!.value
        val deat = Regex("1 DEAT\n2 DATE ([^\n]+)").find(i130)
        assertTrue(deat != null, "I130 hat ein Sterbedatum")
        t = t.replace(i130, i130.replace("1 DEAT\n2 DATE " + deat!!.groupValues[1], "1 DEAT\n2 DATE 1 JAN 1999"))
        val i1 = Regex("(?s)0 @I1@ INDI.*?(?=\n0 @)").find(t)!!.value
        t = t.replace(i1 + "\n", "")
        val i2 = Regex("(?s)0 @I2@ INDI.*?(?=\n0 @)").find(t)!!.value
        val name2 = Regex("1 NAME ([^\n]+)").find(i2)!!.groupValues[1]
        t = t.replace(i2, i2.replace("0 @I2@ INDI", "0 @I7777@ INDI").replace("1 NAME $name2", "1 NAME " + name2.replace("Catharina", "Katharina")))
        t = t.replace("@I2@", "@I7777@")
        t = t.replace("0 TRLR", "0 @I9999@ INDI\n1 NAME Neu /Dazu/\n1 SEX M\n1 BIRT\n2 DATE 1900\n0 TRLR")
        val b = GedcomLeser.alsExport(GedcomLeser.zerlegen(t), jetzt = 2026)
        val v = GedcomVergleich.vergleichen(a, b)
        // I130 geaendert (Tod)
        val p130 = v.paare.single { it.a == "I130" }
        assertEquals(PaarArt.Geaendert, p130.art)
        assertTrue(p130.aenderungen.any { it.feld == "DEAT" && it.b.startsWith("1 JAN 1999") }, p130.aenderungen.joinToString { it.feld + ":" + it.a + ">" + it.b })
        // I1 nur in A, I9999 nur in B
        assertTrue("I1" in v.nurA, v.nurA.toString())
        assertTrue("I9999" in v.nurB, v.nurB.toString())
        // I2 -> I7777 ueber den Schluessel oder als wahrscheinlich
        val p2 = v.paare.single { it.a == "I2" }
        assertEquals("I7777", p2.b)
        // fast alles andere unveraendert
        assertTrue(v.gleich.size > a.individuals.size - 20, "gleich: ${v.gleich.size} von ${a.individuals.size}")
        assertTrue(v.nurA.size <= 3 && v.nurB.size <= 3, "nurA ${v.nurA} nurB ${v.nurB}")
    }
}
