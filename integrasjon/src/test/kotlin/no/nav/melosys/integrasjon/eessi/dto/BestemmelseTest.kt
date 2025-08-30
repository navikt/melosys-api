package no.nav.melosys.integrasjon.eessi.dto

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.eessi.sed.Bestemmelse
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import org.junit.jupiter.api.Test

class BestemmelseTest {

    @Test
    fun `fra melosys bestemmelse`() {
        Bestemmelse.fraMelosysBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1) shouldBe Bestemmelse.ART_12_1
        Bestemmelse.fraMelosysBestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1) shouldBe Bestemmelse.ART_11_4
        Bestemmelse.fraMelosysBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2) shouldBe Bestemmelse.ART_12_2
        Bestemmelse.fraMelosysBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4) shouldBe Bestemmelse.ART_13_1_b_4
        Bestemmelse.fraMelosysBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2) shouldBe Bestemmelse.ART_16_2
    }

    @Test
    fun `til melosys bestemmelse`() {
        Bestemmelse.ART_12_1.tilMelosysBestemmelse() shouldBe Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
        Bestemmelse.ART_11_4.tilMelosysBestemmelse() shouldBe Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4
    }

    @Test
    fun `fra bestemmelse string`() {
        shouldThrow<IllegalArgumentException> { Bestemmelse.fraBestemmelseString(null) }


        Bestemmelse.fraBestemmelseString("12_1") shouldBe Bestemmelse.ART_12_1


        shouldThrow<IllegalArgumentException> { Bestemmelse.fraBestemmelseString("") }
    }
}