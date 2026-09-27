package de.bgghome.webtrees.nativ.pruefung

/*
 * Datum fuer die Plausibilitaetspruefung (27.09.2026): aus der GEDCOM-Form eines Fakts ("12 MAR 1890", "ABT 1850",
 * "BET 1840 AND 1845", "BEF 1900", "@#DJULIAN@ 3 FEB 1700/01") ein Zeitraum in julianischen Tagen.
 * [min]..[max] ist der Zeitraum, in dem das Ereignis sicher liegt; "sicher vor" heisst: max von a < min von b.
 * Ein Punktdatum (auch ungefaehr: ABT, CAL, EST - wie im Pruefprogramm aus db-blank) hat [jahr], [monat] und [tag];
 * Zeitraeume und offene Angaben (BEF, AFT, BET, FROM/TO) haben kein Punktjahr und fallen aus Altersregeln heraus.
 */
class PruefDatum(
    /** Punktjahr; null bei Zeitraeumen und offenen Angaben. */
    val jahr: Int?,
    val monat: Int?,
    val tag: Int?,
    val min: Long,
    val max: Long,
    /** Anzeige, wie der Server sie liefert ("21. Juni 1844"), sonst die GEDCOM-Form. */
    val text: String,
) {
    /** Dasselbe Datum mit anderer Anzeige (geschaetzt aus Taufe oder Begraebnis). */
    fun mitText(t: String) = PruefDatum(jahr, monat, tag, min, max, t)

    /** Tagesgenaues Punktdatum. */
    val voll: Boolean get() = jahr != null && monat != null && tag != null

    /** Laufende Tagesnummer wie im Pruefprogramm aus db-blank (Jahr*372 + Monat*31 + Tag): Abstaende in Jahren/Monaten. */
    internal val tage: Int get() = (jahr ?: 0) * 372 + (monat ?: 1) * 31 + (tag ?: 1)

    /** Dasselbe tagesgenaue Datum. */
    fun gleich(o: PruefDatum?): Boolean = o != null && voll && o.voll && jahr == o.jahr && monat == o.monat && tag == o.tag

    /** Monat 13, 31. April, 29. Februar ohne Schaltjahr ... */
    val unmoeglich: Boolean get() {
        val m = monat ?: return false
        if (m !in 1..12) return true
        val d = tag ?: return false
        return d < 1 || d > tageImMonat(jahr ?: 2000, m)
    }

    companion object {
        private const val OFFEN = 1L shl 40
        private val MONATE = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

        /** Liest die GEDCOM-Form; null, wenn kein Datum darin steckt (reiner Text, fremder Kalender). */
        fun aus(gedcom: String, anzeige: String = ""): PruefDatum? {
            var s = gedcom.trim().uppercase().replace(Regex("\\s+"), " ")
            if (s.isEmpty() || s.startsWith("(")) return null
            val text = anzeige.ifBlank { gedcom.trim() }
            fun teil(t: String) = punkt(t.trim())
            when {
                s.startsWith("BET ") && " AND " in s -> {
                    val (a, b) = s.removePrefix("BET ").split(" AND ", limit = 2).map(::teil)
                    if (a == null || b == null) return null
                    return PruefDatum(null, null, null, a.min, b.max, text)
                }
                s.startsWith("FROM ") -> {
                    val rest = s.removePrefix("FROM ")
                    val (a, b) = if (" TO " in rest) rest.split(" TO ", limit = 2).map(::teil) else listOf(teil(rest), null)
                    a ?: return null
                    return PruefDatum(null, null, null, a.min, b?.max ?: OFFEN, text)
                }
                s.startsWith("TO ") -> { val b = teil(s.removePrefix("TO ")) ?: return null; return PruefDatum(null, null, null, -OFFEN, b.max, text) }
                s.startsWith("BEF ") -> { val b = teil(s.removePrefix("BEF ")) ?: return null; return PruefDatum(null, null, null, -OFFEN, b.min - 1, text) }
                s.startsWith("AFT ") -> { val a = teil(s.removePrefix("AFT ")) ?: return null; return PruefDatum(null, null, null, a.max + 1, OFFEN, text) }
            }
            for (v in listOf("ABT ", "CAL ", "EST ", "INT ")) if (s.startsWith(v)) s = s.removePrefix(v)
            s = s.substringBefore(" (").trim()
            val p = punkt(s) ?: return null
            return PruefDatum(p.jahr, p.monat, p.tag, p.min, p.max, text)
        }

        private class Punkt(val jahr: Int, val monat: Int?, val tag: Int?, val min: Long, val max: Long)

        /** "[@#DJULIAN@] [T] [MON] JJJJ[/JJ] [B.C.]" */
        private fun punkt(roh: String): Punkt? {
            var s = roh
            var julianisch = false
            if (s.startsWith("@#")) {
                val kal = s.substring(0, s.indexOf('@', 2) + 1)
                when (kal) {
                    "@#DGREGORIAN@" -> {}
                    "@#DJULIAN@" -> julianisch = true
                    else -> return null   // franzoesischer Revolutionskalender, hebraeisch ...: nicht pruefen
                }
                s = s.removePrefix(kal).trim()
            }
            val vChr = s.endsWith("B.C.") || s.endsWith("BC")
            if (vChr) return null
            val t = s.split(' ').filter(String::isNotEmpty)
            if (t.isEmpty()) return null
            // Doppeljahr 1720/21 (alter Jahresbeginn): das spaetere Jahr zaehlt
            val jahr = t.last().let { j ->
                val basis = j.substringBefore('/').toIntOrNull() ?: return null
                if ('/' in j) basis + 1 else basis
            }
            val monat = if (t.size >= 2) MONATE.indexOf(t[t.size - 2]).takeIf { it >= 0 }?.plus(1) ?: return null else null
            val tag = if (t.size >= 3) t[t.size - 3].toIntOrNull() ?: return null else null
            if (t.size > 3) return null
            val jd = if (julianisch) ::jdJulianisch else ::jdGregorianisch
            val min = jd(jahr, monat ?: 1, tag ?: 1)
            val max = when {
                tag != null -> min
                monat != null -> jd(jahr, monat, tageImMonat(jahr, monat.coerceIn(1, 12)))
                else -> jd(jahr, 12, 31)
            }
            return Punkt(jahr, monat, tag, min, max)
        }

        fun schaltjahr(y: Int) = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0

        fun tageImMonat(y: Int, m: Int) = when (m) {
            4, 6, 9, 11 -> 30
            2 -> if (schaltjahr(y)) 29 else 28
            else -> 31
        }

        private fun jdGregorianisch(y: Int, m: Int, d: Int): Long {
            val a = (14 - m) / 12
            val yy = y + 4800L - a
            val mm = m + 12 * a - 3
            return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045
        }

        private fun jdJulianisch(y: Int, m: Int, d: Int): Long {
            val a = (14 - m) / 12
            val yy = y + 4800L - a
            val mm = m + 12 * a - 3
            return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - 32083
        }
    }
}

/** a liegt sicher vor b; null, wenn eines fehlt. */
internal fun vor(a: PruefDatum?, b: PruefDatum?): Boolean? = if (a == null || b == null) null else a.max < b.min

/** b - a in Jahren: dezimal bei zwei tagesgenauen Daten, sonst ganze Jahre; null ohne zwei Punktdaten. */
internal fun jahre(a: PruefDatum?, b: PruefDatum?): Double? {
    if (a?.jahr == null || b?.jahr == null) return null
    return if (a.voll && b.voll) (b.tage - a.tage) / 372.0 else (b.jahr - a.jahr).toDouble()
}

/** b - a in Monaten, nur wenn beide mindestens monatsgenau sind. */
internal fun monate(a: PruefDatum?, b: PruefDatum?): Double? {
    if (a?.jahr == null || b?.jahr == null || a.monat == null || b.monat == null) return null
    return (b.jahr - a.jahr) * 12.0 + (b.monat - a.monat) + ((b.tag ?: 1) - (a.tag ?: 1)) / 31.0
}
