package no.nav.melosys.saksflyt.steg.saksopplysninger

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.saksopplysninger.PersonopplysningerLagrer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class LagrePersonopplysningerTest {

    @MockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var personopplysningerLagrer: PersonopplysningerLagrer

    private lateinit var lagrePersonopplysninger: LagrePersonopplysninger

    private val behandlingId = 123L
    private val prosessinstans = Prosessinstans.forTest()
    private lateinit var behandling: Behandling

    @BeforeEach
    fun setup() {
        lagrePersonopplysninger = LagrePersonopplysninger(
            behandlingService,
            personopplysningerLagrer
        )

        behandling = Behandling.forTest {
            id = behandlingId
            fagsak = Fagsak.forTest { medBruker() }
        }

        prosessinstans.behandling = behandling
    }

    @Test
    fun `inngangsSteg returnerer LAGRE_PERSONOPPLYSNINGER`() {
        lagrePersonopplysninger.inngangsSteg() shouldBe ProsessSteg.LAGRE_PERSONOPPLYSNINGER
    }

    @Test
    fun `utfør henter behandling og kaller personopplysningerLagrer`() {
        val freshBehandling = Behandling.forTest {
            id = behandlingId
            fagsak = Fagsak.forTest { medBruker() }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns freshBehandling

        lagrePersonopplysninger.utfør(prosessinstans)

        verify { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) }
        verify { personopplysningerLagrer.lagreHvisMangler(freshBehandling) }
    }

    @Test
    fun `utfør henter alltid behandling på nytt for å få oppdatert tilstand`() {
        val freshBehandling = Behandling.forTest {
            id = behandlingId
            fagsak = Fagsak.forTest { medBruker() }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns freshBehandling

        lagrePersonopplysninger.utfør(prosessinstans)

        // Verifiser at vi bruker den ferske behandlingen, ikke den fra prosessinstans
        verify { personopplysningerLagrer.lagreHvisMangler(freshBehandling) }
    }
}
