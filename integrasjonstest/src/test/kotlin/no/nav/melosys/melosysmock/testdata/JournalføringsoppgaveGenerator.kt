package no.nav.melosys.melosysmock.testdata

import no.nav.melosys.melosysmock.journalpost.journalpostapi.BrukerIdType
import no.nav.melosys.melosysmock.journalpost.journalpostapi.JournalpostApi
import no.nav.melosys.melosysmock.journalpost.journalpostapi.OpprettJournalpostRequest
import no.nav.melosys.melosysmock.oppgave.Oppgave
import no.nav.melosys.melosysmock.oppgave.OppgaveApi
import no.nav.melosys.melosysmock.person.PersonRepo
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class JournalføringsoppgaveGenerator(
    private val journalpostApi: JournalpostApi,
    private val oppgaveApi: OppgaveApi
) {
    fun opprettJfrOppgave(tilordnetRessurs: String, forVirksomhet: Boolean): Oppgave {
        val opprettJournalpostRequest = JournalpostFactory.lagJournalPost(forVirksomhet)
        val journalpostMap = journalpostApi.opprettJournalpost(opprettJournalpostRequest, false)
        return opprettJfrOppgave(
            opprettJournalpostRequest = opprettJournalpostRequest,
            journalpostID = journalpostMap["journalpostId"] as String,
            tilordnetRessurs = tilordnetRessurs
        ).body!!
    }

    private fun opprettJfrOppgave(
        opprettJournalpostRequest: OpprettJournalpostRequest,
        journalpostID: String,
        tilordnetRessurs: String
    ) = oppgaveApi.opprettOppgave(
        Oppgave(
            aktoerId = if (opprettJournalpostRequest.bruker?.idType === BrukerIdType.FNR) PersonRepo.finnVedIdent(
                opprettJournalpostRequest.bruker.id ?: ""
            )?.aktørId else null,
            orgnr = if (opprettJournalpostRequest.bruker?.idType === BrukerIdType.ORGNR) opprettJournalpostRequest.bruker.id else null,
            behandlesAvApplikasjon = "FS38",
            tildeltEnhetsnr = "4530",
            journalpostId = journalpostID,
            beskrivelse = "test",
            oppgavetype = "JFR",
            tema = "MED",
            prioritet = "NORM",
            tilordnetRessurs = tilordnetRessurs,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = LocalDate.now()
        )
    )
}
