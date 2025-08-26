package no.nav.melosys.saksflyt.steg.behandling

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.altinn.AltinnSoeknadService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OpprettFagsakOgBehandlingFraAltinnSøknadTest {

    private val altinnSoeknadService: AltinnSoeknadService = mockk()

    private lateinit var opprettFagsakOgBehandlingFraAltinnSøknad: OpprettFagsakOgBehandlingFraAltinnSøknad

    private val soeknadID = "abc123"
    private val behandling = Behandling.forTest {}
    private val prosessinstans = Prosessinstans.forTest {
        medData(ProsessDataKey.MOTTATT_SOKNAD_ID, soeknadID)
    }

    @BeforeEach
    fun setup() {
        opprettFagsakOgBehandlingFraAltinnSøknad = OpprettFagsakOgBehandlingFraAltinnSøknad(altinnSoeknadService)

        every { altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soeknadID) } returns behandling
    }

    @Test
    fun `utfør skal opprette behandling og verifisere neste steg`() {
        opprettFagsakOgBehandlingFraAltinnSøknad.utfør(prosessinstans)


        verify { altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soeknadID) }
        prosessinstans.behandling shouldBe behandling
    }
}
