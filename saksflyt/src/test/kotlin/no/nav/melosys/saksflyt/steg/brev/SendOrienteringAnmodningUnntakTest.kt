package no.nav.melosys.saksflyt.steg.brev

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK
import no.nav.melosys.saksflyt.brev.BrevBestiller
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.behandling.BehandlingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SendOrienteringAnmodningUnntakTest {

    @MockK
    private lateinit var brevBestiller: BrevBestiller

    @MockK
    private lateinit var behandlingService: BehandlingService

    private lateinit var behandling: Behandling
    private lateinit var prosessinstans: Prosessinstans
    private lateinit var sendOrienteringAnmodningUnntak: SendOrienteringAnmodningUnntak

    @BeforeEach
    fun setUp() {
        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.ANMODNING_OM_UNNTAK
            status = ProsessStatus.KLAR
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
            }
            medData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER)
            medData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, Behandlingsresultattyper.ANMODNING_OM_UNNTAK.kode)
        }
        behandling = prosessinstans.hentBehandling

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandling.id) } returns behandling


        sendOrienteringAnmodningUnntak = SendOrienteringAnmodningUnntak(brevBestiller, behandlingService)
    }

    @Test
    fun utfoerSteg() {
        val captor = slot<DoksysBrevbestilling>()
        every { brevBestiller.bestill(capture(captor)) } returns Unit


        sendOrienteringAnmodningUnntak.utfør(prosessinstans)


        captor.captured.run {
            produserbartdokument shouldBe ORIENTERING_ANMODNING_UNNTAK
            behandling shouldBe this@SendOrienteringAnmodningUnntakTest.behandling
            avsenderID shouldBe SAKSBEHANDLER
            mottakere shouldBe listOf(Mottaker.medRolle(Mottakerroller.BRUKER))
        }
    }

    companion object {
        private const val SAKSBEHANDLER = "Z121212"
    }
}
