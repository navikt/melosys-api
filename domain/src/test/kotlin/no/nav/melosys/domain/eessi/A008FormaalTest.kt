package no.nav.melosys.domain.eessi

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class A008FormaalTest {

    @Test
    fun `fraVerdi returnerer korrekt enum for gyldige verdier`() {
        A008Formaal.fraVerdi("endringsmelding") shouldBe A008Formaal.ENDRINGSMELDING
        A008Formaal.fraVerdi("arbeid_flere_land") shouldBe A008Formaal.ARBEID_FLERE_LAND
    }

    @Test
    fun `fraVerdi returnerer null for ugyldig verdi`() {
        A008Formaal.fraVerdi("ugyldig_verdi").shouldBeNull()
        A008Formaal.fraVerdi("").shouldBeNull()
        A008Formaal.fraVerdi("ENDRINGSMELDING").shouldBeNull() // case-sensitive
    }

    @Test
    fun `fraVerdi returnerer null for null input`() {
        A008Formaal.fraVerdi(null).shouldBeNull()
    }

    @Test
    fun `erGyldig returnerer true for gyldige verdier`() {
        A008Formaal.erGyldig("endringsmelding") shouldBe true
        A008Formaal.erGyldig("arbeid_flere_land") shouldBe true
    }

    @Test
    fun `erGyldig returnerer true for null`() {
        A008Formaal.erGyldig(null) shouldBe true
    }

    @Test
    fun `erGyldig returnerer false for ugyldige verdier`() {
        A008Formaal.erGyldig("ugyldig") shouldBe false
        A008Formaal.erGyldig("") shouldBe false
        A008Formaal.erGyldig("ARBEID_FLERE_LAND") shouldBe false // case-sensitive
    }

    @Test
    fun `gyldigeVerdier returnerer alle gyldige string-verdier`() {
        A008Formaal.gyldigeVerdier() shouldContainExactlyInAnyOrder listOf(
            "endringsmelding",
            "arbeid_flere_land"
        )
    }

    @Test
    fun `enum verdi matcher forventet string`() {
        A008Formaal.ENDRINGSMELDING.verdi shouldBe "endringsmelding"
        A008Formaal.ARBEID_FLERE_LAND.verdi shouldBe "arbeid_flere_land"
    }
}
