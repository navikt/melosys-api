package no.nav.melosys.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class UtenlandskMyndighetTest {

    private fun myndighet(
        gateadresse1: String? = null,
        gateadresse2: String? = null,
        gateadresse3: String? = null
    ) = UtenlandskMyndighet().apply {
        setGateadresse1(gateadresse1)
        setGateadresse2(gateadresse2)
        setGateadresse3(gateadresse3)
    }

    @Test
    fun `gateadresseAsList returnerer alle tre linjer i rekkefølge`() {
        myndighet(
            gateadresse1 = "Social Security Administration",
            gateadresse2 = "4170 Annex Building",
            gateadresse3 = "6401 Security Blvd"
        ).gateadresseAsList shouldBe listOf(
            "Social Security Administration",
            "4170 Annex Building",
            "6401 Security Blvd"
        )
    }

    @Test
    fun `gateadresseAsList hopper over linjer som ikke er satt`() {
        myndighet(gateadresse1 = "Box 1164").gateadresseAsList shouldBe listOf("Box 1164")
        myndighet(gateadresse1 = "Box 1164", gateadresse3 = "Visby").gateadresseAsList shouldBe
            listOf("Box 1164", "Visby")
    }

    @Test
    fun `kombinertGateadresse slår sammen alle satte linjer med komma`() {
        myndighet(
            gateadresse1 = "Social Security Administration",
            gateadresse2 = "4170 Annex Building",
            gateadresse3 = "6401 Security Blvd"
        ).kombinertGateadresse shouldBe
            "Social Security Administration, 4170 Annex Building, 6401 Security Blvd"
    }

    @Test
    fun `kombinertGateadresse returnerer kun første linje når de øvrige mangler`() {
        myndighet(gateadresse1 = "Eläketurvakeskus").kombinertGateadresse shouldBe "Eläketurvakeskus"
    }

    @Test
    fun `kombinertGateadresse gir null når ingen linjer er satt`() {
        myndighet().kombinertGateadresse shouldBe null
    }
}
