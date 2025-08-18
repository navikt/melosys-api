package no.nav.melosys.service.behandling

import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingEndretAvSaksbehandlerEvent
import no.nav.melosys.domain.dokument.DokumentBestiltEvent
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering
import no.nav.melosys.service.oppgave.OppgaveFactory
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class BehandlingEventListenerTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var oppgaveService: OppgaveService

    private lateinit var behandlingEventListener: BehandlingEventListener

    private val oppgaveFactory = OppgaveFactory()

    private val behandling = Behandling.forTest {
        id = BEHANDLING_ID
    }

    @BeforeEach
    fun setup() {
        behandlingEventListener = BehandlingEventListener(behandlingService, oppgaveService)
    }

    @Test
    fun `dokument bestilt, dokument er innvilgelsesbrev, ingen aksjon`() {
        behandlingEventListener.dokumentBestilt(DokumentBestiltEvent(BEHANDLING_ID, Produserbaredokumenter.INNVILGELSE_YRKESAKTIV))


        verify(exactly = 0) { behandlingService.hentBehandling(any()) }
    }

    @Test
    fun `dokument bestilt, dokument er mangelbrev til bruker behandling ikke aktiv, ingen aksjon`() {
        behandling.status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling


        behandlingEventListener.dokumentBestilt(DokumentBestiltEvent(BEHANDLING_ID, Produserbaredokumenter.MANGELBREV_BRUKER))


        verify { behandlingService.hentBehandling(BEHANDLING_ID) }
        verify(exactly = 1) { behandlingService.hentBehandling(any()) }
    }

    @Test
    fun `dokument bestilt, dokument er mangelbrev til bruker behandling er aktiv, oppdaterer status og frist`() {
        behandling.status = Behandlingsstatus.UNDER_BEHANDLING
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingService.oppdaterStatusOgSvarfrist(any(), any(), any()) } just Runs


        behandlingEventListener.dokumentBestilt(DokumentBestiltEvent(BEHANDLING_ID, Produserbaredokumenter.MANGELBREV_BRUKER))


        verify { behandlingService.oppdaterStatusOgSvarfrist(eq(behandling), eq(Behandlingsstatus.AVVENT_DOK_PART), any()) }
    }

    @Test
    fun `dokument bestilt, dokument er mangelbrev til arbeidsgiver behandling er aktiv, oppdaterer status og frist`() {
        behandling.status = Behandlingsstatus.UNDER_BEHANDLING
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingService.oppdaterStatusOgSvarfrist(any(), any(), any()) } just Runs


        behandlingEventListener.dokumentBestilt(DokumentBestiltEvent(BEHANDLING_ID, Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER))


        verify { behandlingService.oppdaterStatusOgSvarfrist(eq(behandling), eq(Behandlingsstatus.AVVENT_DOK_PART), any()) }
    }

    @Test
    fun `behandling endret, oppdaterer oppgave, med riktig data`() {
        behandling.apply {
            type = Behandlingstyper.NY_VURDERING
            tema = Behandlingstema.IKKE_YRKESAKTIV
            behandlingsfrist = LocalDate.of(2022, 3, 7)
        }
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        val behandlingEndretAvSaksbehandlerEvent = BehandlingEndretAvSaksbehandlerEvent(
            BEHANDLING_ID,
            behandling
        )
        val oppgave = Oppgave.Builder().setOppgaveId(OPPGAVE_ID).build()
        every { oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(behandling.fagsak.saksnummer) } returns Optional.of(oppgave)
        every { oppgaveService.lagBehandlingsoppgave(behandling) } returns oppgaveFactory.lagBehandlingsoppgave(
            behandling,
            LocalDate.now(),
            behandling::hentSedDokument
        )
        every { oppgaveService.oppdaterOppgave(any(), any<OppgaveOppdatering>()) } just Runs


        behandlingEventListener.behandlingEndret(behandlingEndretAvSaksbehandlerEvent)


        val behandlingsOppgaveForType = oppgaveService.lagBehandlingsoppgave(behandling).build()
        val capturedOppgaveOppdatering = slot<OppgaveOppdatering>()
        verify { oppgaveService.oppdaterOppgave(eq(OPPGAVE_ID), capture(capturedOppgaveOppdatering)) }
        capturedOppgaveOppdatering.captured.apply {
            behandlingstema shouldBe behandlingsOppgaveForType.behandlingstema
            tema shouldBe behandlingsOppgaveForType.tema
            fristFerdigstillelse shouldBe LocalDate.of(2022, 3, 7)
            beskrivelse shouldBe behandlingsOppgaveForType.beskrivelse
            oppgavetype shouldBe behandlingsOppgaveForType.oppgavetype
        }
    }

    companion object {
        private const val BEHANDLING_ID = 123321L
        private const val OPPGAVE_ID = "333"
    }
}
