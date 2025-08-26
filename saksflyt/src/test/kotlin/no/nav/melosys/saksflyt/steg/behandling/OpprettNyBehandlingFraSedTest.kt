package no.nav.melosys.saksflyt.steg.behandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class OpprettNyBehandlingFraSedTest {

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var joarkFasade: JoarkFasade

    @MockK
    private lateinit var oppgaveFasade: OppgaveService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    private val mottaksdato = LocalDate.EPOCH

    private lateinit var opprettNyBehandlingFraSed: OpprettNyBehandlingFraSed

    @BeforeEach
    fun setup() {
        opprettNyBehandlingFraSed = OpprettNyBehandlingFraSed(fagsakService, behandlingService, oppgaveFasade, joarkFasade, behandlingsresultatService)
    }

    @Test
    fun `utfør skal kaste TekniskException når gsakSaksnummer ikke er satt`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
        }


        val exception = assertThrows<TekniskException> {
            opprettNyBehandlingFraSed.utfør(prosessinstans)
        }


        exception.message shouldBe "ArkivsakID kan ikke være null"
    }

    @Test
    fun `utfør skal kaste TekniskException når behandlingstype ikke er satt`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.GSAK_SAK_ID, 123L)
        }


        val exception = assertThrows<TekniskException> {
            opprettNyBehandlingFraSed.utfør(prosessinstans)
        }


        exception.message shouldBe "Behandlingstema kan ikke være null"
    }

    @Test
    fun `utfør skal opprette ny behandling når har tidligere behandling og oppgave`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.GSAK_SAK_ID, 123L)
            medData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING)
            medData(ProsessDataKey.JOURNALPOST_ID, "jp123")
            medData(ProsessDataKey.DOKUMENT_ID, "dok123")
            medData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding().apply {
                this.journalpostId = "jp123"
                this.dokumentId = "dok123"
            })
        }

        val behandling = Behandling.forTest {
            id = 123L
            status = Behandlingsstatus.UNDER_BEHANDLING
            fagsak { }
        }

        val oppgave = Oppgave.Builder()
            .setOppgaveId("123oppg")
            .build()

        every { fagsakService.hentFagsakFraArkivsakID(123L) } returns behandling.fagsak
        every { fagsakService.lagre(any<Fagsak>()) } just Runs
        every { behandlingService.nyBehandling(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Behandling.forTest { }
        every { joarkFasade.hentMottaksDatoForJournalpost("jp123") } returns mottaksdato
        every { oppgaveFasade.finnÅpenBehandlingsoppgaveMedFagsaksnummer(any()) } returns Optional.of(oppgave)
        every { oppgaveFasade.ferdigstillOppgave(any()) } just Runs
        every { behandlingService.avsluttBehandling(any()) } just Runs
        every { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), any()) } just Runs


        opprettNyBehandlingFraSed.utfør(prosessinstans)


        verify { oppgaveFasade.ferdigstillOppgave(oppgave.oppgaveId) }
        verify { behandlingService.avsluttBehandling(behandling.id) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.id, Behandlingsresultattyper.FERDIGBEHANDLET) }
        verify { behandlingService.nyBehandling(
            behandling.fagsak, Behandlingsstatus.UNDER_BEHANDLING, Behandlingstyper.NY_VURDERING,
            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, "jp123", "dok123",
            mottaksdato, Behandlingsaarsaktyper.SED, null) }
        prosessinstans.behandling shouldNotBe null
    }
}
