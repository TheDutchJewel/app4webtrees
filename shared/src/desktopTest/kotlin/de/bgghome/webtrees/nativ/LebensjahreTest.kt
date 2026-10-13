package de.bgghome.webtrees.nativ

import de.bgghome.webtrees.nativ.api.DateJson
import de.bgghome.webtrees.nativ.api.EventJson
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.desk.lebensjahre
import kotlin.test.Test
import kotlin.test.assertEquals

class LebensjahreTest {
    private fun ev(j: Int) = EventJson(date = DateJson(year = j))
    private fun p(geb: Int?, tod: Int?, tot: Boolean) =
        Person(xref = "I1", isDead = tot, birth = geb?.let(::ev), death = tod?.let(::ev))

    @Test fun verstorben() = assertEquals("1850 – 1925", lebensjahre(p(1850, 1925, true)))
    @Test fun lebend() = assertEquals("1950 –", lebensjahre(p(1950, null, false)))
    @Test fun totOhneTodesjahr() = assertEquals("1850 – ?", lebensjahre(p(1850, null, true)))
    @Test fun nurTodesjahr() = assertEquals("– 1925", lebensjahre(p(null, 1925, true)))
    @Test fun nichts() = assertEquals("", lebensjahre(p(null, null, false)))
}
