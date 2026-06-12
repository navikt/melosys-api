package no.nav.melosys.tjenester.gui

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FilnavnTest {

    @Test
    fun `saner skal beholde vanlig tittel uendret`() {
        Filnavn.saner("Vedtak om medlemskap") shouldBe "Vedtak om medlemskap"
    }

    @Test
    fun `saner skal beholde norske tegn`() {
        Filnavn.saner("Søknad om æøå") shouldBe "Søknad om æøå"
    }

    @Test
    fun `saner skal erstatte path-separatorer med understrek`() {
        Filnavn.saner("foo/bar\\baz") shouldBe "foo_bar_baz"
    }

    @Test
    fun `saner skal fjerne kontrolltegn`() {
        Filnavn.saner("linje1\nlinje2\ttab\r") shouldBe "linje1linje2tab"
    }

    @Test
    fun `saner skal fjerne DEL og C1-kontrolltegn`() {
        val medKontrolltegn = "a" + 0x7F.toChar() + "b" + 0x9F.toChar() + "c"
        Filnavn.saner(medKontrolltegn) shouldBe "abc"
    }
}
