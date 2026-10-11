package de.bgghome.webtrees.nativ

import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.data.vornamen
import kotlin.test.Test
import kotlin.test.assertEquals

class SpitznameTest {
    private fun p(name: String, given: String, surname: String) = Person(xref = "I1", name = name, given = given, surname = surname)

    @Test fun nickHinterDenVornamen() = assertEquals(
        "Frederika Louise Wilhelmina \"Louise\"",
        p("Frederika Louise Wilhelmina \"Louise\" van Oranje Nassau", "Frederika Louise Wilhelmina", "van Oranje Nassau").vornamen,
    )

    @Test fun nickZwischenDenVornamen() = assertEquals(
        "Johannes \"Jan\" Peter",
        p("Johannes \"Jan\" Peter de Vries", "Johannes Peter", "de Vries").vornamen,
    )

    @Test fun ohneNick() = assertEquals("Johannes", p("Johannes de Vries", "Johannes", "de Vries").vornamen)

    @Test fun nurNick() = assertEquals("\"Jan\"", p("\"Jan\" … de Vries", "", "de Vries").vornamen)

    @Test fun mitTitelVorn() = assertEquals(
        "Johannes \"Jan\"",
        p("Dr. Johannes \"Jan\" de Vries", "Johannes", "de Vries").vornamen,
    )
}
