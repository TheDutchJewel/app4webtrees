package de.bgghome.webtrees.nativ.data

/*
 * Kirchenjahr fuer Genealogen: Kirchenbuecher datieren oft nach Sonntagen und Festen ("Dominica Laetare 1734",
 * "am 3. Sonntag nach Trinitatis"). Hier: Ostern (gregorianisch und julianisch), alle Sonntage und Feste eines
 * Kalenderjahres, Datum -> Name und Name -> Datum. Namen deutsch und lateinisch; evangelische Zaehlung nach Trinitatis,
 * katholische nach Pfingsten. Regionale Brauche weichen ab - die Liste ist eine Naeherung wie jedes gedruckte Verzeichnis.
 */
object Kirchenjahr {
    data class Tag(val datum: Kalender.Datum, val name: String, val latein: String = "", val beweglich: Boolean = true, val zuOstern: Int? = null)

    /** Ostersonntag; julianisch: Datum im julianischen Kalender. */
    fun ostern(jahr: Int, julianisch: Boolean): Kalender.Datum {
        if (julianisch) {
            val a = jahr % 4; val b = jahr % 7; val c = jahr % 19
            val d = (19 * c + 15) % 30
            val e = (2 * a + 4 * b - d + 34) % 7
            val monat = (d + e + 114) / 31
            val tag = (d + e + 114) % 31 + 1
            return Kalender.Datum(jahr, monat, tag, true)
        }
        val a = jahr % 19; val b = jahr / 100; val c = jahr % 100
        val d = b / 4; val e = b % 4; val f = (b + 8) / 25; val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4; val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val monat = (h + l - 7 * m + 114) / 31
        val tag = (h + l - 7 * m + 114) % 31 + 1
        return Kalender.Datum(jahr, monat, tag, false)
    }

    /** Bewegliche Tage relativ zu Ostern: Abstand, deutscher Name, lateinischer Name. */
    private val UM_OSTERN = listOf(
        Triple(-63, "Septuagesimae", "Dominica Septuagesima"),
        Triple(-56, "Sexagesimae", "Dominica Sexagesima"),
        Triple(-49, "Estomihi", "Dominica Quinquagesima (Esto mihi)"),
        Triple(-47, "Fastnacht", "Feria III ante Cineres"),
        Triple(-46, "Aschermittwoch", "Feria IV Cinerum"),
        Triple(-42, "Invokavit", "Dominica Invocavit"),
        Triple(-35, "Reminiszere", "Dominica Reminiscere"),
        Triple(-28, "Okuli", "Dominica Oculi"),
        Triple(-21, "Lätare", "Dominica Laetare"),
        Triple(-14, "Judika", "Dominica Judica"),
        Triple(-7, "Palmarum", "Dominica Palmarum"),
        Triple(-3, "Gründonnerstag", "Feria V in Coena Domini"),
        Triple(-2, "Karfreitag", "Feria VI in Parasceve"),
        Triple(0, "Ostersonntag", "Dominica Resurrectionis (Pascha)"),
        Triple(1, "Ostermontag", "Feria II Paschae"),
        Triple(7, "Quasimodogeniti", "Dominica Quasimodogeniti (in Albis)"),
        Triple(14, "Misericordias Domini", "Dominica Misericordias Domini"),
        Triple(21, "Jubilate", "Dominica Jubilate"),
        Triple(28, "Kantate", "Dominica Cantate"),
        Triple(35, "Rogate", "Dominica Rogate"),
        Triple(39, "Christi Himmelfahrt", "Ascensio Domini"),
        Triple(42, "Exaudi", "Dominica Exaudi"),
        Triple(49, "Pfingstsonntag", "Dominica Pentecostes"),
        Triple(50, "Pfingstmontag", "Feria II Pentecostes"),
        Triple(56, "Trinitatis", "Dominica Trinitatis"),
    )

    /** Feste Feiertage (Monat, Tag, Name, Latein). */
    private val FEST = listOf(
        listOf(1, 1, "Neujahr (Beschneidung Christi)", "Circumcisio Domini"),
        listOf(1, 6, "Epiphanias (Heilige Drei Könige)", "Epiphania Domini"),
        listOf(2, 2, "Mariä Lichtmess (Reinigung)", "Purificatio Mariae"),
        listOf(2, 24, "Matthias", "S. Matthiae Apostoli"),
        listOf(3, 25, "Mariä Verkündigung", "Annuntiatio Mariae"),
        listOf(6, 24, "Johannis (Johannes der Täufer)", "Nativitas S. Joannis Baptistae"),
        listOf(6, 29, "Peter und Paul", "SS. Petri et Pauli"),
        listOf(7, 2, "Mariä Heimsuchung", "Visitatio Mariae"),
        listOf(7, 25, "Jakobi", "S. Jacobi Apostoli"),
        listOf(8, 10, "Laurentius", "S. Laurentii"),
        listOf(8, 15, "Mariä Himmelfahrt", "Assumptio Mariae"),
        listOf(8, 24, "Bartholomäus", "S. Bartholomaei Apostoli"),
        listOf(9, 8, "Mariä Geburt", "Nativitas Mariae"),
        listOf(9, 29, "Michaelis", "S. Michaelis Archangeli"),
        listOf(10, 31, "Reformationsfest", ""),
        listOf(11, 1, "Allerheiligen", "Omnium Sanctorum"),
        listOf(11, 2, "Allerseelen", "Commemoratio Omnium Fidelium Defunctorum"),
        listOf(11, 11, "Martini", "S. Martini Episcopi"),
        listOf(12, 6, "Nikolaus", "S. Nicolai Episcopi"),
        listOf(12, 21, "Thomä (Apostel)", "S. Thomae Apostoli"),
        listOf(12, 24, "Heiligabend", "Vigilia Nativitatis"),
        listOf(12, 25, "Christtag (1. Weihnachtstag)", "Nativitas Domini"),
        listOf(12, 26, "Stephanstag (2. Weihnachtstag)", "S. Stephani Protomartyris"),
        listOf(12, 28, "Unschuldige Kinder", "SS. Innocentium"),
        listOf(12, 31, "Silvester", "S. Silvestri"),
    )

    private fun ord(n: Int) = "$n."
    private fun roemisch(n: Int): String {
        val werte = listOf(10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
        var r = n; val sb = StringBuilder()
        for ((w, z) in werte) while (r >= w) { sb.append(z); r -= w }
        return sb.toString()
    }

    /**
     * Alle Sonntage und Feste des Kalenderjahres [jahr], nach Datum. [katholisch]: Sonntage nach Pfingsten statt nach
     * Trinitatis, dazu Fronleichnam; evangelisch: Buß- und Bettag. Alle Daten im gewaehlten Kalender.
     */
    fun tage(jahr: Int, julianisch: Boolean, katholisch: Boolean = false): List<Tag> {
        val liste = mutableListOf<Tag>()
        fun jd(m: Int, t: Int) = Kalender.Datum(jahr, m, t, julianisch).jd()
        fun datum(jd: Int) = Kalender.ausJd(jd, julianisch)
        val o = ostern(jahr, julianisch).jd()
        UM_OSTERN.forEach { (d, n, l) ->
            val name = if (katholisch && d == 56) "Dreifaltigkeitssonntag" else n
            liste += Tag(datum(o + d), name, l, true, d)
        }
        if (katholisch) liste += Tag(datum(o + 60), "Fronleichnam", "Corpus Christi", true, 60)

        // 1. Advent dieses Jahres: Sonntag vom 27.11. bis 3.12.
        val advent1 = (jd(11, 27)..jd(12, 3)).first { Kalender.wochentag(it) == 7 }
        // Sonntage nach Trinitatis (ev.) bzw. nach Pfingsten (kath.) bis vor dem Ewigkeitssonntag/letzten Sonntag
        var n = 1
        var s = o + 56 + 7
        while (s < advent1 - 7) {
            val name = if (katholisch) "${ord(n + 1)} Sonntag nach Pfingsten" else "${ord(n)} Sonntag nach Trinitatis"
            val latein = if (katholisch) "Dominica ${roemisch(n + 1)} post Pentecosten" else "Dominica ${roemisch(n)} post Trinitatis"
            liste += Tag(datum(s), name, latein, true, s - o)
            s += 7; n++
        }
        liste += Tag(datum(advent1 - 7), if (katholisch) "Christkönigssonntag (letzter Sonntag)" else "Ewigkeitssonntag (Totensonntag)", "Dominica ultima post Trinitatis", true)
        if (!katholisch) liste += Tag(datum(advent1 - 11), "Buß- und Bettag", "", true)
        for (k in 0..3) liste += Tag(datum(advent1 + 7 * k), "${ord(k + 1)} Advent", "Dominica ${roemisch(k + 1)} Adventus", true)

        // Sonntage nach Epiphanias: vom ersten Sonntag nach dem 6.1. bis vor Septuagesimae
        var e = jd(1, 7)
        while (Kalender.wochentag(e) != 7) e++
        var k = 1
        while (e < o - 63) {
            liste += Tag(datum(e), "${ord(k)} Sonntag nach Epiphanias", "Dominica ${roemisch(k)} post Epiphaniam", true, e - o)
            e += 7; k++
        }
        // Sonntag nach Neujahr (zwischen 2. und 5.1.)
        (jd(1, 2)..jd(1, 5)).firstOrNull { Kalender.wochentag(it) == 7 }?.let { liste += Tag(datum(it), "Sonntag nach Neujahr", "Dominica post Circumcisionem", true) }
        // Sonntage nach Weihnachten (26.–31.12.)
        (jd(12, 26)..jd(12, 31)).firstOrNull { Kalender.wochentag(it) == 7 }?.let { liste += Tag(datum(it), "Sonntag nach Weihnachten", "Dominica infra Octavam Nativitatis", true) }

        FEST.forEach { f ->
            val name = f[2] as String
            if (katholisch && name.startsWith("Reformation")) return@forEach
            if (!katholisch && (name.startsWith("Allerseelen"))) return@forEach
            liste += Tag(Kalender.Datum(jahr, f[0] as Int, f[1] as Int, julianisch), name, f[3] as String, false)
        }
        return liste.sortedBy { it.datum.jd() }
    }

    /** Was ein Datum im Kirchenjahr ist: Fest/Sonntag am Tag selbst, sonst "Mittwoch nach Okuli". */
    fun benennen(d: Kalender.Datum, katholisch: Boolean = false, wochentage: List<String> = WOCHENTAGE_DE): List<String> {
        val jd = d.jd()
        val liste = tage(d.jahr, d.julianisch, katholisch) + tage(d.jahr - 1, d.julianisch, katholisch).filter { it.datum.monat == 12 }
        val amTag = liste.filter { it.datum.jd() == jd }.map { t -> t.name + if (t.latein.isNotBlank()) " – " + t.latein else "" }
        if (amTag.isNotEmpty()) return amTag
        val sonntag = liste.filter { it.beweglich && Kalender.wochentag(it.datum.jd()) == 7 && it.datum.jd() < jd && jd - it.datum.jd() < 7 }.maxByOrNull { it.datum.jd() }
        return listOfNotNull(sonntag?.let { "${wochentage[Kalender.wochentag(jd) - 1]} nach ${it.name}" })
    }

    val WOCHENTAGE_DE = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")

    /**
     * Name -> Datum: sucht im Jahr nach einem Sonntag oder Fest ("Laetare", "Dom. Oculi", "3. Sonntag nach Trinitatis",
     * "Trin. 3", "Dominica III post Trinitatis"). Liefert alle Treffer.
     */
    fun suchen(text: String, jahr: Int, julianisch: Boolean, katholisch: Boolean = false): List<Tag> {
        val q = normal(text)
        if (q.isBlank()) return emptyList()
        val zahl = Regex("""\b(\d{1,2})\b""").find(q)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""\b([ivx]{1,5})\b""").find(q)?.groupValues?.get(1)?.let(::ausRoemisch)
        val woerter = q.split(' ').filter { it.isNotBlank() && it.toIntOrNull() == null && ausRoemisch(it) == null }
            .filterNot { it in FUELL }
        return tage(jahr, julianisch, katholisch).filter { t ->
            val n = normal(t.name + " " + t.latein)
            val nummer = Regex("""\b(\d{1,2})\b""").find(normal(t.name))?.groupValues?.get(1)?.toIntOrNull()
            woerter.all { w -> n.split(' ').any { it.startsWith(w) } } && (zahl == null || nummer == zahl)
        }
    }

    private val FUELL = setOf("dominica", "dom", "sonntag", "so", "am", "nach", "post", "p", "der", "des", "die", "feria", "fest", "tag")

    private fun ausRoemisch(s: String): Int? {
        val w = mapOf('i' to 1, 'v' to 5, 'x' to 10)
        if (s.isEmpty() || s.any { it !in w }) return null
        var sum = 0
        for (i in s.indices) { val v = w[s[i]]!!; val nx = if (i + 1 < s.length) w[s[i + 1]]!! else 0; sum += if (v < nx) -v else v }
        return sum.takeIf { it in 1..30 }
    }

    private fun normal(s: String): String = s.lowercase()
        .replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss")
        .replace("ae", "a").replace("oe", "o").replace("ue", "u")
        .replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
        // gleichwertige Schreibweisen (Okuli/Oculi, Kantate/Cantate, Invokavit/Invocavit, Judika/Judica, Latare/Laetare)
        .replace("oculi", "okuli").replace("cantate", "kantate").replace("invocavit", "invokavit").replace("judica", "judika")
        .replace("reminiscere", "reminiszere").replace("trinitatis", "trinitatis").replace("trin ", "trinitatis ").let { if (it.endsWith(" trin")) it + "itatis" else it }
}
