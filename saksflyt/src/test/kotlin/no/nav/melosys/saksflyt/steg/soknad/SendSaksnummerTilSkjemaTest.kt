package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.Runs
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.integrasjon.melosysskjema.MelosysSkjemaApiClient
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class SendSaksnummerTilSkjemaTest {

    @MockK
    lateinit var melosysSkjemaApiClient: MelosysSkjemaApiClient

    private lateinit var sendSaksnummerTilSkjema: SendSaksnummerTilSkjema

    private val saksnummer = "MEL-1234"

    @BeforeEach
    fun setup() {
        sendSaksnummerTilSkjema = SendSaksnummerTilSkjema(melosysSkjemaApiClient)
    }

    @Test
    fun `inngangsSteg returnerer SEND_SAKSNUMMER_TIL_SKJEMA`() {
        sendSaksnummerTilSkjema.inngangsSteg() shouldBe ProsessSteg.SEND_SAKSNUMMER_TIL_MELOSYS_SKJEMA_API
    }

    @Test
    fun `utfør kaller registrerSaksnummer med riktig skjemaId og saksnummer`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto()
        val skjemaId = søknadsdata.skjema.id

        val behandling = Behandling.forTest {
            fagsak { this.saksnummer = this@SendSaksnummerTilSkjemaTest.saksnummer }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.DIGITAL_SØKNADSDATA, søknadsdata)
            this.behandling = behandling
        }

        every { melosysSkjemaApiClient.registrerSaksnummer(skjemaId, saksnummer) } just Runs

        sendSaksnummerTilSkjema.utfør(prosessinstans)

        verify(exactly = 1) { melosysSkjemaApiClient.registrerSaksnummer(skjemaId, saksnummer) }
    }
}
