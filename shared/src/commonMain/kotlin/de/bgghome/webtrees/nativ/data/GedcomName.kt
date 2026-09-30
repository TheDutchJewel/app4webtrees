package de.bgghome.webtrees.nativ.data

/**
 * Ein GEDCOM-Name ("Jonas Heinrich /Falkenrath/ jun.") in seinen Teilen: vor den Schraegstrichen die Vornamen,
 * dazwischen der Familienname, dahinter ein Zusatz. Ohne Schraegstriche gilt alles als Vornamen.
 */
data class GedcomName(val vornamen: String, val familienname: String, val zusatz: String) {
    /** Zurueck ins GEDCOM; der Familienname steht immer zwischen Schraegstrichen, auch leer, wenn es Vornamen gibt. */
    fun gedcom(): String = listOf(vornamen.trim(), if (familienname.isBlank() && vornamen.isNotBlank() && zusatz.isBlank()) "" else "/${familienname.trim()}/", zusatz.trim())
        .filter(String::isNotEmpty).joinToString(" ")

    /** Zum Lesen: "Falkenrath, Jonas Heinrich jun." - wie im Register; ohne Familienname nur die Vornamen. */
    fun anzeige(): String = listOf(
        listOf(familienname.trim(), vornamen.trim()).filter(String::isNotEmpty).joinToString(", "),
        zusatz.trim(),
    ).filter(String::isNotEmpty).joinToString(" ")

    companion object {
        fun aus(wert: String): GedcomName {
            val a = wert.indexOf('/')
            val b = if (a >= 0) wert.indexOf('/', a + 1) else -1
            if (a < 0) return GedcomName(wert.trim(), "", "")
            val ende = if (b < 0) wert.length else b
            return GedcomName(wert.substring(0, a).trim(), wert.substring(a + 1, ende).trim(), if (b < 0) "" else wert.substring(b + 1).trim())
        }
    }
}
