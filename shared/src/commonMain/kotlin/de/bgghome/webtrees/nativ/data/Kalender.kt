package de.bgghome.webtrees.nativ.data

/*
 * Rechnen mit Daten fuer Genealogen: julianischer und gregorianischer Kalender ueber die Julianische Tageszahl (JD),
 * Abstand zweier Daten in Jahren/Monaten/Tagen, Geburtsdatum aus Sterbedatum und Alter ("alt 63 J. 4 M. 2 T."),
 * Wochentag, Umrechnung zwischen den Kalendern. Reine Rechnung ohne Oberflaeche.
 */
object Kalender {
    /** Ein Datum in einem bestimmten Kalender. */
    data class Datum(val jahr: Int, val monat: Int, val tag: Int, val julianisch: Boolean = false) {
        fun jd(): Int = if (julianisch) jdJulianisch(jahr, monat, tag) else jdGregorianisch(jahr, monat, tag)
        fun gedcom(): String = (if (julianisch) "@#DJULIAN@ " else "") + "$tag ${MONATE[monat - 1]} $jahr"
        override fun toString(): String = tag.toString().padStart(2, '0') + "." + monat.toString().padStart(2, '0') + "." + jahr
    }

    private val MONATE = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

    fun jdGregorianisch(j: Int, m: Int, t: Int): Int {
        val a = (14 - m) / 12
        val y = j + 4800 - a
        val mm = m + 12 * a - 3
        return t + (153 * mm + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    }

    fun jdJulianisch(j: Int, m: Int, t: Int): Int {
        val a = (14 - m) / 12
        val y = j + 4800 - a
        val mm = m + 12 * a - 3
        return t + (153 * mm + 2) / 5 + 365 * y + y / 4 - 32083
    }

    fun ausJd(jd: Int, julianisch: Boolean): Datum {
        val c: Int
        val b: Int
        if (julianisch) {
            b = 0; c = jd + 32082
        } else {
            val a = jd + 32044
            b = (4 * a + 3) / 146097
            c = a - 146097 * b / 4
        }
        val d = (4 * c + 3) / 1461
        val e = c - 1461 * d / 4
        val m = (5 * e + 2) / 153
        val tag = e - (153 * m + 2) / 5 + 1
        val monat = m + 3 - 12 * (m / 10)
        val jahr = 100 * b + d - 4800 + m / 10
        return Datum(jahr, monat, tag, julianisch)
    }

    fun schaltjahr(jahr: Int, julianisch: Boolean): Boolean =
        if (julianisch) jahr % 4 == 0 else (jahr % 4 == 0 && jahr % 100 != 0) || jahr % 400 == 0

    fun tageImMonat(jahr: Int, monat: Int, julianisch: Boolean): Int = when (monat) {
        2 -> if (schaltjahr(jahr, julianisch)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    fun gueltig(d: Datum): Boolean = d.monat in 1..12 && d.tag in 1..tageImMonat(d.jahr, d.monat, d.julianisch) && d.jahr in -4000..9999

    /** Wochentag 1 = Montag … 7 = Sonntag. */
    fun wochentag(jd: Int): Int = jd.mod(7) + 1

    /** Dasselbe Datum im anderen Kalender. */
    fun umrechnen(d: Datum): Datum = ausJd(d.jd(), !d.julianisch)

    /**
     * Eingabe wie in den Datumsfeldern ("17.05.1924", "1924-05-17", "17 MAY 1924", "17. Mai 1924"); [julianisch] gilt, wenn
     * die Eingabe den Kalender nicht selbst nennt (@#DJULIAN@). Nur volle Tagesdaten; sonst null.
     */
    fun lesen(eingabe: String, julianisch: Boolean = false): Datum? {
        val t = eingabe.trim()
        if (t.isEmpty()) return null
        Regex("""^(\d{3,4})-(\d{1,2})-(\d{1,2})$""").find(t)?.let { m ->
            return Datum(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt(), julianisch).takeIf(::gueltig)
        }
        val jul = t.contains("@#DJULIAN@", ignoreCase = true)
        val g = GedcomDate.fromInput(t.replace("@#DJULIAN@", "", ignoreCase = true)).trim()
        val m = Regex("""^(\d{1,2}) ([A-Z]{3}) (\d{1,4})$""").find(g) ?: return null
        val monat = MONATE.indexOf(m.groupValues[2]) + 1
        if (monat == 0) return null
        return Datum(m.groupValues[3].toInt(), monat, m.groupValues[1].toInt(), julianisch || jul).takeIf(::gueltig)
    }

    /** Abstand in vollen Jahren, Monaten, Tagen (von [a] nach [b], beide in ihrem Kalender) und in Tagen. */
    data class Abstand(val jahre: Int, val monate: Int, val tage: Int, val tageGesamt: Int, val negativ: Boolean)

    fun abstand(a: Datum, b: Datum): Abstand {
        val ja = a.jd(); val jb = b.jd()
        val negativ = jb < ja
        // Gerechnet wird im Kalender des fruehen Datums, beide Daten dorthin umgerechnet
        val julianisch = (if (negativ) b else a).julianisch
        val von = ausJd(minOf(ja, jb), julianisch)
        val bis = ausJd(maxOf(ja, jb), julianisch)
        var jahre = bis.jahr - von.jahr
        var monate = bis.monat - von.monat
        var tage = bis.tag - von.tag
        if (tage < 0) {
            monate--
            val vorMonat = if (bis.monat == 1) 12 else bis.monat - 1
            val vorJahr = if (bis.monat == 1) bis.jahr - 1 else bis.jahr
            tage += tageImMonat(vorJahr, vorMonat, julianisch)
        }
        if (monate < 0) { jahre--; monate += 12 }
        return Abstand(jahre, monate, tage, kotlin.math.abs(jb - ja), negativ)
    }

    /**
     * Geburtsdatum aus Sterbedatum und Alter: erst Jahre und Monate kalendermaessig abziehen (Monatsende bleibt Monatsende),
     * dann Wochen und Tage als Tageszahl. So rechnen die Kirchenbuecher ("alt 63 J. 4 M. 2 T.").
     */
    fun geburt(tod: Datum, jahre: Int, monate: Int, wochen: Int = 0, tage: Int = 0): Datum {
        var j = tod.jahr - jahre
        var m = tod.monat - monate
        while (m < 1) { m += 12; j-- }
        val t = minOf(tod.tag, tageImMonat(j, m, tod.julianisch))
        val zwischen = Datum(j, m, t, tod.julianisch)
        return ausJd(zwischen.jd() - wochen * 7 - tage, tod.julianisch)
    }
}
