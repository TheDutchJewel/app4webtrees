package de.bgghome.webtrees.nativ.data

import de.bgghome.webtrees.nativ.api.Person

private val SPITZNAME = Regex("\"[^\"]+\"")
private val LEER = Regex("\\s+")

/**
 * Vornamen fuer die Anzeige, mit dem Spitznamen in Anfuehrungszeichen. webtrees laesst den Spitznamen in den
 * Vornamen (givn) weg, gleich ob er im NAME steht oder als 2 NICK; der volle Name traegt ihn. Steht er dort
 * zwischen den Vornamen, bleibt er an seiner Stelle, sonst kommt er hinter die Vornamen.
 * Fuer Sortieren, Suchen und Regeln weiter [Person.given] nehmen.
 */
val Person.vornamen: String
    get() {
        val nick = SPITZNAME.find(name)?.value ?: return given
        if (given.contains(nick)) return given
        if (given.isBlank()) return nick
        val vorne = if (surname.isNotBlank() && name.contains(surname)) name.substringBefore(surname).trim() else ""
        fun norm(s: String) = s.replace(LEER, " ").trim()
        if (vorne.contains(nick) && norm(vorne.replace(nick, " ")) == norm(given)) return vorne
        val beiGiven = name.indexOf(given)
        return if (beiGiven > name.indexOf(nick)) "$nick $given" else "$given $nick"
    }
