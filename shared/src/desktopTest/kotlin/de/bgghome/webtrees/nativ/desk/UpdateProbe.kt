package de.bgghome.webtrees.nativ.desk

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Probe gegen die echte Homepage (WT_UPDATEPROBE=1): version.txt wird gelesen und richtig verglichen. */
class UpdateProbe {
    @Test fun live() {
        if (System.getenv("WT_UPDATEPROBE") == null) return
        val v = runBlocking { UpdatePruefung.abrufen("wtTux/dev (Updateprobe)") }
        println("version.txt: $v")
        assertNotNull(v)
        assertEquals(UpdatePruefung.Ergebnis.Neuer(v), runBlocking { UpdatePruefung.jetzt("1.0", "wtTux/dev (Updateprobe)") })
        assertEquals(UpdatePruefung.Ergebnis.Aktuell, runBlocking { UpdatePruefung.jetzt(v, "wtTux/dev (Updateprobe)") })
    }
}
