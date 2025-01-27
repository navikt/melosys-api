package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.extension.Extension
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.kodeverk.Avsendertyper
import no.nav.melosys.domain.kodeverk.ForvaltningsmeldingMottaker
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.melosysmock.oppgave.Oppgave
import no.nav.melosys.melosysmock.testdata.JournalføringsoppgaveGenerator
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.journalforing.dto.*
import no.nav.melosys.service.oppgave.OppgaveService
import java.time.LocalDate

class JournalfoeringBase(
    protected val journalføringsoppgaveGenerator: JournalføringsoppgaveGenerator,
    protected val journalføringService: JournalfoeringService,
    protected val oppgaveService: OppgaveService,
    extensionForWireMock: Extension? = null
) : MockServerTestBaseWithProsessManager(extensionForWireMock) {

    protected fun journalførOgVentTilProsesserErFerdige(
        journalfoeringOpprettDto: JournalfoeringOpprettDto,
        waitFor: Map<ProsessType, Int>,
    ): Prosessinstans {
        val jfrOppgave: Oppgave = lagJfrOppgave()
        val lagJournalfoeringOpprettDto = lagJournalfoeringOpprettDto(jfrOppgave, journalfoeringOpprettDto)

        return executeAndWait(waitFor, ProsessType.JFR_NY_SAK_BRUKER) {
            journalføringService.journalførOgOpprettSak(lagJournalfoeringOpprettDto)
            oppgaveService.ferdigstillOppgave(lagJournalfoeringOpprettDto.oppgaveID)
        }
    }

    protected fun executeAndWait(
        waitForProsesses: Map<ProsessType, Int>,
        returnProsessOfType: ProsessType = waitForProsesses.keys.first(),
        process: () -> Unit
    ): Prosessinstans = prosessinstansTestManager.executeAndWait(
        waitForProsesses = waitForProsesses,
        returnProsessOfType = returnProsessOfType,
        process = process)

    protected fun lagJfrOppgave(): Oppgave =
        journalføringsoppgaveGenerator.opprettJfrOppgave(tilordnetRessurs = "Z123456", forVirksomhet = false)

    protected fun lagJournalfoeringOpprettDto(
        jfrOppgave: Oppgave,
        journalfoeringOpprettDto: JournalfoeringOpprettDto
    ): JournalfoeringOpprettDto {

        val hentJournalpost = if (jfrOppgave.journalpostId != null) journalføringService.hentJournalpost(jfrOppgave.journalpostId) else null
        return lagJournalføringDto(jfrOppgave, hentJournalpost?.hoveddokument, journalfoeringOpprettDto)
    }

    private fun lagJournalføringDto(
        oppgave: Oppgave,
        dokument: ArkivDokument?,
        journalfoeringOpprettDto: JournalfoeringOpprettDto
    ): JournalfoeringOpprettDto {
        return journalfoeringOpprettDto.apply {
            this.journalpostID = oppgave.journalpostId
            oppgaveID = oppgave.id.toString()
            hoveddokument = DokumentDto(dokument?.dokumentId, dokument?.tittel).apply {
                logiskeVedlegg = emptyList()
            }
        }.apply {
            behandlingstemaKode.shouldNotBeNull()
            behandlingstypeKode.shouldNotBeNull()
            fagsak.sakstype.shouldNotBeNull()
            fagsak.sakstema.shouldNotBeNull()
        }
    }

    protected fun defaultJournalføringDto() =
        JournalfoeringOpprettDto().apply {
            avsenderID = "30056928150"
            avsenderNavn = "KARAFFEL TRIVIELL"
            brukerID = "30056928150"
            virksomhetOrgnr = null
            vedlegg = emptyList()
            mottattDato = LocalDate.now()
            forvaltningsmeldingMottaker = ForvaltningsmeldingMottaker.BRUKER
            avsenderType = Avsendertyper.PERSON
            isSkalTilordnes = true
            fagsak = FagsakDto().apply {
                soknadsperiode = PeriodeDto(
                    periodeFOM,
                    periodeTOM,
                )
                land = SoeknadslandDto(
                    listOf(Landkoder.IE.kode), false
                )
            }
        }

    protected fun lagJournalfoeringOppgaveOgTilordneDto(
        saksnummer: String,
        journalfoeringTilordneDto: JournalfoeringTilordneDto = defaultJournalfoeringTilordneDto()
    ): JournalfoeringTilordneDto {
        val jfrOppgave: Oppgave = lagJfrOppgave()
        val hentJournalpost = if (jfrOppgave.journalpostId != null) journalføringService.hentJournalpost(jfrOppgave.journalpostId) else null
        return journalfoeringTilordneDto
            .apply {
                this.saksnummer = saksnummer
                journalpostID = jfrOppgave.journalpostId
                oppgaveID = jfrOppgave.id.toString()
                hoveddokument = DokumentDto(hentJournalpost?.hoveddokument?.dokumentId, hentJournalpost?.hoveddokument?.tittel).apply {
                    logiskeVedlegg = emptyList()
                }
            }.apply {
                behandlingstemaKode.shouldNotBeNull()
                behandlingstypeKode.shouldNotBeNull()
            }
    }

    protected fun defaultJournalfoeringTilordneDto() =
        JournalfoeringTilordneDto().apply {
            avsenderID = "30056928150"
            avsenderNavn = "KARAFFEL TRIVIELL"
            brukerID = "30056928150"
            virksomhetOrgnr = null
            vedlegg = emptyList()
            mottattDato = LocalDate.now()
            forvaltningsmeldingMottaker = ForvaltningsmeldingMottaker.BRUKER
            avsenderType = Avsendertyper.PERSON
            isSkalTilordnes = true
        }

    companion object {
        val periodeFOM = LocalDate.of(2001, 1, 1)
        val periodeTOM = LocalDate.of(2001, 1, 2)
    }
}
