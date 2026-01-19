package no.nav.melosys.saksflyt.steg.brev

import io.mockk.*
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.begrunnelse
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.saksflyt.brev.BrevBestiller
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private const val BEHANDLING_ID = 12314L

@ExtendWith(MockKExtension::class)
class SendHenleggelsesbrevTest {

    private val brevBestiller: BrevBestiller = mockk()
    private val behandlingsresultatService: BehandlingsresultatService = mockk()

    private lateinit var sendHenleggelsesbrev: SendHenleggelsesbrev

    @BeforeEach
    fun setUp() {
        sendHenleggelsesbrev = SendHenleggelsesbrev(brevBestiller, behandlingsresultatService)
        every { brevBestiller.bestill(any(), any(), any(), any(), any(), any()) } just Runs
    }

    @Test
    fun `utfør sendHenleggelsesbrev produserDokument`() {
        val saksbehandler = "Z097"

        val behandlingsresultat = Behandlingsresultat.forTest {
            begrunnelseFritekst = "fritekst"
            begrunnelse(Henleggelsesgrunner.ANNET.kode)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.HENLEGG_SAK
            status = ProsessStatus.KLAR
            behandling {
                id = BEHANDLING_ID
                fagsak {
                    gsakSaksnummer = FagsakTestFactory.GSAK_SAKSNUMMER
                }
            }
            medData(ProsessDataKey.BEGRUNNELSE_FRITEKST, "fritekst")
            medData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
        }

        sendHenleggelsesbrev.utfør(prosessinstans)

        verify {
            brevBestiller.bestill(
                Produserbaredokumenter.MELDING_HENLAGT_SAK,
                setOf(Mottaker.medRolle(Mottakerroller.BRUKER)),
                behandlingsresultat.begrunnelseFritekst,
                any<String>(),
                Henleggelsesgrunner.ANNET.kode,
                prosessinstans.behandling
            )
        }
    }
}
