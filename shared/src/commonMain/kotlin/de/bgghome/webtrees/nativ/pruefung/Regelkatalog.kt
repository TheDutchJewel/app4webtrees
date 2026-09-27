package de.bgghome.webtrees.nativ.pruefung

/*
 * Der Regelkatalog der Plausibilitaetspruefung. Kennnummern wie im Pruefprogramm von db-blank
 * (0xx Chronologie, 1xx Altersgrenzen, 2xx Struktur, 3xx Namen, 4xx Quellen); 5xx Orte ist neu (Stufe 2, 27.09.2026).
 * Die Texte stehen hier zweisprachig, damit der Motor ohne Compose-Ressourcen laeuft (Tests).
 */

enum class Schwere { Fehler, Warnung }

/** Einheit des Grenzwerts. */
enum class Einheit { Jahre, Monate, Anzahl, Kilometer }

enum class RegelGruppe { Chronologie, Alter, Struktur, Namen, Quellen, Orte }

class Regel(
    val id: String,
    val gruppe: RegelGruppe,
    val schwere: Schwere,
    /** Voreingestellter Grenzwert; null: Regel ohne Grenzwert. */
    val grenzwert: Double?,
    val einheit: Einheit?,
    private val de: String,
    private val en: String,
    /** Von Haus aus ausgeschaltet (meldet in vielen Baeumen sehr viel). */
    val standardAus: Boolean = false,
) {
    fun frage(deutsch: Boolean) = if (deutsch) de else en
}

object Regelkatalog {
    private val C = RegelGruppe.Chronologie
    private val A = RegelGruppe.Alter
    private val S = RegelGruppe.Struktur
    private val NA = RegelGruppe.Namen
    private val Q = RegelGruppe.Quellen
    private val O = RegelGruppe.Orte
    private val F = Schwere.Fehler
    private val W = Schwere.Warnung
    private val J = Einheit.Jahre
    private val M = Einheit.Monate
    private val N = Einheit.Anzahl
    private val KM = Einheit.Kilometer

    val alle: List<Regel> = listOf(
        Regel("010", C, F, null, null, "Stirbt jemand vor seiner Geburt?", "Does someone die before being born?"),
        Regel("011", C, F, null, null, "Heiratet jemand vor seiner Geburt?", "Does someone marry before being born?"),
        Regel("012", C, F, null, null, "Heiratet jemand nach seinem Tod?", "Does someone marry after their death?"),
        Regel("013", C, F, null, null, "Wird ein Kind vor seiner Mutter geboren?", "Is a child born before its mother?"),
        Regel("014", C, F, null, null, "Wird ein Kind vor seinem Vater geboren?", "Is a child born before its father?"),
        Regel("015", C, F, 0.0, M, "Wird ein Kind nach dem Tod der Mutter geboren?", "Is a child born after its mother's death?"),
        Regel("016", C, F, 9.5, M, "Wird ein Kind später als die Grenze nach dem Tod des Vaters geboren?", "Is a child born later than the limit after its father's death?"),
        Regel("017", C, W, 1.0, J, "Wird ein Kind mehr als die Grenze vor der Ehe der Eltern geboren?", "Is a child born more than the limit before its parents' marriage?"),
        Regel("018", C, F, null, null, "Gibt es Daten, die es nicht gibt (31. April, 30. Februar …)?", "Are there dates that do not exist (31 April, 30 February …)?"),
        Regel("019", C, F, null, null, "Liegt ein Datum in der Zukunft?", "Is a date in the future?"),
        Regel("020", C, F, null, null, "Wird jemand vor seiner Geburt getauft?", "Is someone baptised before being born?"),
        Regel("021", C, F, null, null, "Wird jemand vor seinem Tod begraben?", "Is someone buried before their death?"),
        Regel("022", C, W, null, null, "Ist das Geburtsdatum gleich dem Heiratsdatum?", "Is the date of birth the same as the date of marriage?"),
        Regel("023", C, W, null, null, "Stehen die Kinder einer Familie nicht in Geburtsreihenfolge?", "Are the children of a family out of birth order?"),
        Regel("024", C, W, null, null, "Ist ein Pate oder Zeuge beim Ereignis schon tot oder noch nicht geboren?", "Is a godparent or witness already dead or not yet born at the event?"),

        Regel("110", A, F, 110.0, J, "Wird jemand älter als die Grenze?", "Does someone live longer than the limit?"),
        Regel("111", A, F, 14.0, J, "Ist eine Mutter bei der Geburt jünger als die Grenze?", "Is a mother younger than the limit at the birth?"),
        Regel("112", A, F, 55.0, J, "Ist eine Mutter bei der Geburt älter als die Grenze?", "Is a mother older than the limit at the birth?"),
        Regel("113", A, F, 14.0, J, "Ist ein Vater bei der Geburt jünger als die Grenze?", "Is a father younger than the limit at the birth?"),
        Regel("114", A, W, 75.0, J, "Ist ein Vater bei der Geburt älter als die Grenze?", "Is a father older than the limit at the birth?"),
        Regel("115", A, F, 15.0, J, "Heiratet jemand jünger als die Grenze?", "Does someone marry younger than the limit?"),
        Regel("116", A, W, 90.0, J, "Heiratet jemand älter als die Grenze?", "Does someone marry older than the limit?"),
        Regel("117", A, W, 40.0, J, "Liegen zwischen Ehepartnern mehr Jahre als die Grenze?", "Are spouses further apart in age than the limit?"),
        Regel("118", A, W, 8.0, M, "Liegen zwei Geschwister näher beieinander als die Grenze (keine Zwillinge)?", "Are two siblings closer together than the limit (not twins)?"),
        Regel("119", A, W, 30.0, J, "Liegen zwischen ältestem und jüngstem Kind mehr Jahre als die Grenze?", "Are the oldest and youngest child further apart than the limit?"),
        Regel("120", A, W, 16.0, N, "Hat eine Ehe mehr Kinder als die Grenze?", "Does a marriage have more children than the limit?"),
        Regel("121", A, W, 3.0, N, "Hat jemand mehr Ehen als die Grenze?", "Does someone have more marriages than the limit?"),
        Regel("122", A, W, 2.0, M, "Heiratet jemand früher als die Grenze nach dem Tod des vorigen Partners wieder?", "Does someone remarry sooner than the limit after the previous spouse's death?"),
        Regel("123", A, F, 20.0, N, "Hat eine Mutter über alle Ehen mehr Kinder als die Grenze?", "Does a mother have more children than the limit over all marriages?"),
        Regel("124", A, W, 12.0, J, "Liegen zwischen zwei aufeinanderfolgenden Geschwistern mehr Jahre als die Grenze?", "Are two consecutive siblings further apart than the limit?"),
        Regel("125", A, F, null, null, "Heiratet jemand nach seinem eigenen Sterbedatum?", "Does someone marry after their own date of death?"),
        Regel("126", A, W, 12.0, J, "Ist ein Pate oder Zeuge beim Ereignis jünger als die Grenze?", "Is a godparent or witness younger than the limit at the event?"),
        Regel("127", A, W, 110.0, J, "Wäre jemand ohne Sterbeangabe heute älter als die Grenze?", "Would someone without any death record be older than the limit today?"),

        Regel("210", S, F, null, null, "Gibt es Personen ohne Namen, ohne Daten und ohne jede Verbindung?", "Are there people without name, dates or any link?"),
        Regel("211", S, W, null, null, "Gibt es Personen, die in keiner Familie vorkommen?", "Are there people who belong to no family?"),
        Regel("212", S, W, null, null, "Gibt es Partner ohne jede eigene Angabe in einer kinderlosen Ehe?", "Are there spouses without any details in a childless marriage?"),
        Regel("213", S, W, null, null, "Ist dieselbe Person Kind in zwei Familien?", "Is the same person a child in two families?"),
        Regel("214", S, F, null, null, "Ist jemand Kind in der Familie, in der er selbst Elternteil ist?", "Is someone a child in the family where they are a parent?"),
        Regel("215", S, F, null, null, "Haben beide Ehepartner dasselbe Geschlecht?", "Do both spouses have the same sex?"),
        Regel("216", S, F, null, null, "Steht eine Frau als Vater oder ein Mann als Mutter?", "Is a woman recorded as father or a man as mother?"),
        Regel("217", S, F, null, null, "Gibt es Familien ohne Partner und ohne Kinder?", "Are there families without spouses and children?"),
        Regel("219", S, W, null, null, "Gibt es zwei Personen mit gleichem Namen und gleichem vollen Geburtsdatum?", "Are there two people with the same name and the same full date of birth?"),
        Regel("220", S, F, null, null, "Sind Ehepartner Geschwister?", "Are spouses siblings?"),
        Regel("221", S, F, null, null, "Sind Ehepartner Elternteil und Kind?", "Are spouses parent and child?"),
        Regel("222", S, F, null, null, "Heiratet jemand, während der vorige Partner noch lebt?", "Does someone marry while the previous spouse is still alive?"),
        Regel("223", S, W, null, null, "Haben beide Ehepartner denselben Nachnamen?", "Do both spouses have the same surname?"),
        Regel("224", S, W, null, null, "Haben zwei lebende Geschwister denselben Vornamen?", "Do two living siblings have the same given name?"),
        Regel("225", S, F, null, null, "Ist jemand sein eigener Vorfahr?", "Is someone their own ancestor?"),
        Regel("226", S, W, null, null, "Gibt es dasselbe Elternpaar als zwei Familien?", "Is the same couple recorded as two families?"),
        Regel("228", S, W, 2.0, J, "Gibt es ähnliche Namen mit fast gleichem Geburtsjahr (mögliche Dublette)?", "Are there similar names with almost the same year of birth (possible duplicate)?"),
        Regel("319", S, W, null, null, "Hat ein Ehepartner unbekanntes Geschlecht?", "Does a spouse have an unknown sex?"),

        Regel("310", NA, W, null, null, "Steht im Nachnamensfeld ein weiblicher Rufname (Maria, Anna …)?", "Is there a female given name in the surname field (Maria, Anna …)?"),
        Regel("311", NA, W, null, null, "Steht im Namensfeld ein Platzhalter (unbekannt, NN, ?)?", "Is there a placeholder in a name field (unknown, NN, ?)?"),
        Regel("315", NA, W, null, null, "Steht im Namensfeld eine Zahl oder ein Datum?", "Is there a number or date in a name field?"),
        Regel("330", NA, W, null, null, "Passt das Geschlecht nicht zum Rufnamen?", "Does the sex not fit the given name?"),

        Regel("420", Q, W, null, null, "Gibt es Geburt, Taufe, Heirat, Tod oder Begräbnis ohne Quelle?", "Are there births, baptisms, marriages, deaths or burials without a source?", standardAus = true),
        Regel("421", Q, W, null, null, "Gibt es Personen ganz ohne Quelle?", "Are there people without any source?", standardAus = true),

        Regel("510", O, W, null, null, "Gibt es denselben Ort in verschiedenen Schreibweisen?", "Is the same place written in different ways?"),
        Regel("511", O, W, null, null, "Gibt es Orte ohne Koordinaten?", "Are there places without coordinates?", standardAus = true),
        Regel("512", O, W, 100.0, KM, "Ist jemand am selben Tag an zwei Orten, die weiter als die Grenze auseinander liegen?", "Is someone at two places further apart than the limit on the same day?"),
        Regel("513", O, W, null, null, "Hat ein Ortsname leere Teile (doppeltes Komma)?", "Does a place name have empty parts (double comma)?"),
    )

    val nachId: Map<String, Regel> = alle.associateBy { it.id }

    /** Was ohne eigene Wahl ausgeschaltet ist. */
    val standardAus: Set<String> = alle.filter { it.standardAus }.map { it.id }.toSet()
}

/** Eine Voreinstellung: Grenzwerte, ausgeschaltete Regeln und abweichende Schwere; Schaetzen an oder aus. */
class Voreinstellung(
    val id: String,
    private val de: String,
    private val en: String,
    val grenzwerte: Map<String, Double>,
    val aus: Set<String>,
    val schwere: Map<String, Schwere> = emptyMap(),
    val schaetzen: Boolean = true,
) {
    fun name(deutsch: Boolean) = if (deutsch) de else en

    companion object {
        val alle = listOf(
            Voreinstellung("standard", "Ausgewogen", "Balanced", emptyMap(), Regelkatalog.standardAus),
            // eng: meldet frueh, auch Ungewoehnliches (Werte angelehnt an verbreitete Werkseinstellungen)
            Voreinstellung("streng", "Streng", "Strict",
                mapOf("110" to 100.0, "111" to 16.0, "112" to 50.0, "113" to 16.0, "114" to 70.0, "115" to 16.0, "116" to 80.0,
                    "117" to 25.0, "118" to 9.0, "119" to 25.0, "120" to 14.0, "124" to 10.0, "126" to 14.0, "127" to 100.0, "228" to 3.0, "512" to 50.0),
                setOf("511"), mapOf("017" to Schwere.Fehler, "222" to Schwere.Fehler, "330" to Schwere.Fehler)),
            // weit: nur, was wirklich nicht sein kann
            Voreinstellung("grosszuegig", "Großzügig", "Lenient",
                mapOf("110" to 120.0, "111" to 10.0, "112" to 58.0, "113" to 10.0, "114" to 90.0, "115" to 12.0, "116" to 110.0,
                    "117" to 60.0, "119" to 40.0, "120" to 20.0, "121" to 5.0, "124" to 20.0, "126" to 7.0, "127" to 120.0, "512" to 300.0),
                setOf("017", "022", "023", "211", "223", "224", "228", "310", "311", "420", "421", "511", "513")),
            // Kirchenbuch (18./19. Jh.): Taufe steht fuer die Geburt, Paten zaehlen, Reihenfolge der Kinder ist Darstellung
            Voreinstellung("kirchenbuch", "Kirchenbuch", "Parish register",
                mapOf("111" to 14.0, "112" to 52.0, "113" to 16.0, "114" to 80.0, "118" to 9.0, "122" to 1.0, "126" to 12.0),
                setOf("023", "223", "420", "421", "511"), mapOf("024" to Schwere.Fehler)),
        )
    }
}
