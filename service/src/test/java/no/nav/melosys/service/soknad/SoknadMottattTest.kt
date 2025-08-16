package no.nav.melosys.service.soknad

import io.kotest.matchers.shouldBe
import no.nav.melosys.integrasjon.SoknadMottatt
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class SoknadMottattTest {

    @Test
    fun `mindreEnn7dagerSidenMottak skal returnere false for erForGammelTilForvaltningsmelding`() {
        val soknadMottatt = SoknadMottatt("ID", ZonedDateTime.now().minusDays(6).minusHours(23))

        soknadMottatt.erForGammelTilForvaltningsmelding() shouldBe false
    }

    @Test
    fun `merEnn7dagerSidenMottak skal returnere true for erForGammelTilForvaltningsmelding`() {
        val soknadMottatt = SoknadMottatt("ID", ZonedDateTime.now().minusDays(7).minusHours(1))

        soknadMottatt.erForGammelTilForvaltningsmelding() shouldBe true
    }
} 