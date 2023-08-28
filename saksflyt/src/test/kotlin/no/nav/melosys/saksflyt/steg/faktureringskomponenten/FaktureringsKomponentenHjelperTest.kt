package no.nav.melosys.saksflyt.steg.faktureringskomponenten

import io.kotest.matchers.shouldBe
import no.nav.melosys.service.ftrl.FaktureringsKomponentenHjelper
import kotlin.test.Test

class FaktureringsKomponentenHjelperTest {

    @Test
    fun `mapper til vedtaksid`() {
        val saksnummer = "MEL-23"
        val behandlingsId = "123412"

        FaktureringsKomponentenHjelper.konverterTilVedtaksId(saksnummer, behandlingsId.toLong())
            .shouldBe("$saksnummer-$behandlingsId")
    }

    @Test
    fun `henter behandlingsid fra vedtaksid`() {
        val behandlingsId = "123412".toLong()
        val vedtaksId = "MEL-23-123412"

        FaktureringsKomponentenHjelper.hentBehandingsId(vedtaksId).shouldBe(behandlingsId)
    }

    @Test
    fun `henter saksnummer fra vedtaksid`() {
        val saksnummer = "MEL-23"
        val vedtaksId = "MEL-23-123412"

        FaktureringsKomponentenHjelper.hentSaksnummer(vedtaksId).shouldBe(saksnummer)
    }
}
