package de.bgghome.webtrees.nativ.desk

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdatePruefungTest {
    @Test fun vergleich() {
        assertTrue(UpdatePruefung.istNeuer("1.52", "1.51"))
        assertTrue(UpdatePruefung.istNeuer("1.49.1", "1.49"))
        assertTrue(UpdatePruefung.istNeuer("2.0", "1.99"))
        assertTrue(UpdatePruefung.istNeuer(" 1.52\n", "1.51.3"))
        assertFalse(UpdatePruefung.istNeuer("1.51", "1.51"))
        assertFalse(UpdatePruefung.istNeuer("1.50", "1.51"))
        assertFalse(UpdatePruefung.istNeuer("1.51", "1.51.374"), "Build-Nummer der exe zaehlt nicht als aelter")
        assertFalse(UpdatePruefung.istNeuer("", "1.51"))
        assertFalse(UpdatePruefung.istNeuer("abc", "1.51"))
        assertTrue(UpdatePruefung.istNeuer("1.52", "1.51-test"))
    }
}
