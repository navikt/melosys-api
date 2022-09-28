package no.nav.melosys.itest

import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.kodeverk.Avsendertyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.melosysmock.oppgave.Oppgave
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.journalforing.dto.DokumentDto
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDate
import java.time.LocalDateTime

@Import(KodeverkStub::class)
class JournalfoeringReplikeringIT(
    @Autowired testDataGenerator: TestDataGenerator,
    @Autowired journalføringService: JournalfoeringService,
    @Autowired oppgaveService: OppgaveService,
    @Autowired prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val unleash: FakeUnleash
) : JournalfoeringBase(testDataGenerator, journalføringService, oppgaveService, prosessinstansRepository) {

    @Test
    fun journalførOgOpprettAndregangsBehandling_replikerBehandling_replikerBehandlingProsessStegBlirKjørt() {
        unleash.enable("melosys.behandle_alle_saker")
        val behandling = setupForReplikeringAvBehandling()
        val jfrOppgave: Oppgave = lagJfrOppgave()
        val journalfoeringTilordneDto = lagJournalfoeringTilordneDto(jfrOppgave, behandling.fagsak.saksnummer)

        ThreadLocalAccessInfo.executeProcess("journalførOgOpprettAndregangsBehandling") {
            journalføringService.journalførOgOpprettAndregangsBehandling(journalfoeringTilordneDto)
        }

        //TODO: verify (Debugget og ser at steg blir kjørt så langt)
    }

    private fun setupForReplikeringAvBehandling(): Behandling {
        val startTime = LocalDateTime.now()
        val journalfoeringOpprettDto = lagJournalfoeringOpprettDto()

        journalførOgOpprettSak(journalfoeringOpprettDto)

        val journalføringProsessID = waitForProsesses(startTime)
        val behandling = sjekkBehandlingOgBehandlingsgrunnlag(journalføringProsessID)

        behandling.status = Behandlingsstatus.AVSLUTTET
        behandlingRepository.save(behandling)
        return behandling
    }

    private fun lagJournalfoeringTilordneDto(jfrOppgave: Oppgave, saksnummer: String): JournalfoeringTilordneDto {
        var hentJournalpost: Journalpost? = null
        ThreadLocalAccessInfo.executeProcess("hentJournalpost") {
            hentJournalpost = journalføringService.hentJournalpost(jfrOppgave.journalpostId)
        }
        return lagJournalfoeringTilordneDto(jfrOppgave, hentJournalpost!!.hoveddokument, saksnummer)
    }

    private fun lagJournalfoeringTilordneDto(oppgave: Oppgave, dokument: ArkivDokument, saksnummer: String) =
        JournalfoeringTilordneDto().apply {
            this.saksnummer = saksnummer
            this.journalpostID = oppgave.journalpostId
            avsenderID = "30056928150"
            avsenderNavn = "KARAFFEL TRIVIELL"
            brukerID = "30056928150"
            virksomhetOrgnr = null
            oppgaveID = oppgave.id.toString()
            vedlegg = emptyList()
            mottattDato = LocalDate.now()
            behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
            behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
            isIkkeSendForvaltingsmelding = false
            avsenderType = Avsendertyper.PERSON
            isSkalTilordnes = true
            hoveddokument = DokumentDto(dokument.dokumentId, dokument.tittel).apply {
                logiskeVedlegg = emptyList()
            }
        }
}
