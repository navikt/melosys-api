package no.nav.melosys.saksflyt.steg.behandling

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SettVurderDokumentTest {

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingService: BehandlingService

    private lateinit var settVurderDokument: SettVurderDokument

    private val behandlingID = 21321L
    private lateinit var prosessinstans: Prosessinstans

    @BeforeEach
    fun setUp() {
        every { behandlingService.endreStatus(behandlingID, any<Behandlingsstatus>()) } returns Unit
        settVurderDokument = SettVurderDokument(fagsakService, behandlingService)
        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER)
        }
    }

    @Test
    fun `utfør sakMedBehandling oppdatererStatus`() {
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsakMedBehandling()
        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER)
            medData(ProsessDataKey.JFR_INGEN_VURDERING, false)
        }


        settVurderDokument.utfør(prosessinstans)


        verify { behandlingService.endreStatus(behandlingID, Behandlingsstatus.VURDER_DOKUMENT) }
    }

    @Test
    fun `utfør sakUtenBehandling ingenStatusEndring`() {
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns Fagsak.forTest { }
        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER)
            medData(ProsessDataKey.JFR_INGEN_VURDERING, false)
        }


        settVurderDokument.utfør(prosessinstans)


        verify(exactly = 0) { behandlingService.endreStatus(any<Long>(), any<Behandlingsstatus>()) }
    }

    @Test
    fun `utfør ingenVurdering ingenStatusEndring`() {
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsakMedBehandling()
        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER)
            medData(ProsessDataKey.JFR_INGEN_VURDERING, true)
        }


        settVurderDokument.utfør(prosessinstans)


        verify(exactly = 0) { behandlingService.endreStatus(any<Long>(), any<Behandlingsstatus>()) }
    }

    private fun fagsakMedBehandling() = Fagsak.forTest {
        behandling {
            id = behandlingID
            status = Behandlingsstatus.UNDER_BEHANDLING
        }
    }
}
