package no.nav.melosys.saksflyt.steg.brev

import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.saksflyt.brev.BrevBestiller
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.behandling.BehandlingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SendOrienteringsbrevVideresendSøknadTest {

    private val behandlingService: BehandlingService = mockk()
    private val brevBestiller: BrevBestiller = mockk()

    private lateinit var steg: SendOrienteringsbrevVideresendSøknad
    private lateinit var prosessinstans: Prosessinstans

    private val captor = slot<DoksysBrevbestilling>()

    @BeforeEach
    fun setup() {
        steg = SendOrienteringsbrevVideresendSøknad(behandlingService, brevBestiller)
        every { brevBestiller.bestill(capture(captor)) } just Runs

        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling {
                id = 1L
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns prosessinstans.hentBehandling
    }

    @Test
    fun `utfør brevbestilling harRiktigBrevTypeOgMottaker`() {
        steg.utfør(prosessinstans)

        verify { brevBestiller.bestill(capture(captor)) }
        val brevbestilling = captor.captured

        brevbestilling.produserbartdokument shouldBe Produserbaredokumenter.ORIENTERING_VIDERESENDT_SOEKNAD
        brevbestilling.mottakere?.map { it.rolle } shouldBe listOf(Mottakerroller.BRUKER)
        brevbestilling.behandling shouldBe prosessinstans.hentBehandling
    }
}
