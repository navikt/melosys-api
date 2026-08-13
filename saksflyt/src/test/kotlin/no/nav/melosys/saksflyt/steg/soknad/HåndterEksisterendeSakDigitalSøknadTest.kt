package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class HåndterEksisterendeSakDigitalSøknadTest {

    @MockK lateinit var eksisterendeSakHåndterer: DigitalSøknadEksisterendeSakHåndterer

    private lateinit var steg: HåndterEksisterendeSakDigitalSøknad

    private val saksnummer = "MEL-1234"
    private val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto()

    @BeforeEach
    fun setup() {
        steg = HåndterEksisterendeSakDigitalSøknad(eksisterendeSakHåndterer)
    }

    @Test
    fun `inngangsSteg returnerer HÅNDTER_EKSISTERENDE_SAK_DIGITAL_SØKNAD`() {
        steg.inngangsSteg() shouldBe ProsessSteg.HÅNDTER_EKSISTERENDE_SAK_DIGITAL_SØKNAD
    }

    @Test
    fun `utfør delegerer til håndterer og setter returnert behandling på prosessinstans`() {
        val behandling = mockk<Behandling>(relaxed = true)
        every { eksisterendeSakHåndterer.håndter(saksnummer, søknadsdata) } returns behandling

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.DIGITAL_SØKNADSDATA, søknadsdata)
            medData(ProsessDataKey.SAKSNUMMER, saksnummer)
        }

        steg.utfør(prosessinstans)

        verify { eksisterendeSakHåndterer.håndter(saksnummer, søknadsdata) }
        prosessinstans.behandling shouldBe behandling
    }
}
