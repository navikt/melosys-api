package no.nav.melosys.saksflyt.steg.behandling

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.OpprettSakRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OpprettFagsakOgBehandlingTest {

    @MockK
    private lateinit var fagsakService: FagsakService
    @MockK
    private lateinit var persondataFasade: PersondataFasade

    private lateinit var opprettFagsakOgBehandling: OpprettFagsakOgBehandling

    @BeforeEach
    fun setUp() {
        opprettFagsakOgBehandling = OpprettFagsakOgBehandling(fagsakService, persondataFasade)
    }

    @Test
    fun `utfør skal opprette fagsak når type er JFR ny sak`() {
        val aktørId = "1000104568393"
        val journalpostId = "44553"
        val dokumentId = "222221"

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.JFR_NY_SAK_BRUKER
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.AKTØR_ID, aktørId)
            medData(ProsessDataKey.JOURNALPOST_ID, journalpostId)
            medData(ProsessDataKey.DOKUMENT_ID, dokumentId)
            medData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.UTSENDT_ARBEIDSTAKER)
            medData(ProsessDataKey.SAKSTYPE, Sakstyper.EU_EOS)
            medData(ProsessDataKey.SAKSTEMA, Sakstemaer.MEDLEMSKAP_LOVVALG)
            medData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.FRITEKST)
            medData(ProsessDataKey.BEHANDLINGSÅRSAK_FRITEKST, "Fritekst")
        }

        val fagsak = Fagsak.forTest {
            behandling { }
        }

        val opprettSakRequestSlot = slot<OpprettSakRequest>()
        every { fagsakService.nyFagsakOgBehandling(capture(opprettSakRequestSlot)) } returns fagsak


        opprettFagsakOgBehandling.utfør(prosessinstans)


        verify { fagsakService.nyFagsakOgBehandling(any()) }
        val opprettSakRequest = opprettSakRequestSlot.captured
        opprettSakRequest.run {
            aktørID shouldBe aktørId
            initierendeJournalpostId shouldBe journalpostId
            initierendeDokumentId shouldBe dokumentId
            sakstype shouldBe Sakstyper.EU_EOS
            sakstema shouldBe Sakstemaer.MEDLEMSKAP_LOVVALG
            behandlingsårsaktype shouldBe Behandlingsaarsaktyper.FRITEKST
            behandlingsårsakFritekst shouldBe "Fritekst"
        }
    }
}
