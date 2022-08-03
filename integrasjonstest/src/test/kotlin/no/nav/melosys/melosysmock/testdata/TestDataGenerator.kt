package no.nav.melosys.melosysmock.testdata

import no.nav.melosys.melosysmock.journalpost.journalpostapi.BrukerIdType
import no.nav.melosys.melosysmock.journalpost.journalpostapi.JournalpostApi
import no.nav.melosys.melosysmock.journalpost.journalpostapi.OpprettJournalpostRequest
import no.nav.melosys.melosysmock.oppgave.Oppgave
import no.nav.melosys.melosysmock.oppgave.OppgaveApi
import no.nav.melosys.melosysmock.person.PersonRepo
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate


@RestController
@RequestMapping("/testdata")
class TestDataGenerator(
    private val journalpostApi: JournalpostApi,
    private val oppgaveApi: OppgaveApi,
    private val journalPostService: JournalPostService
) {

    @PostMapping("/jfr-oppgave")
    fun lagJournalføringsoppgave(@RequestBody request: OpprettJfrOppgaveRequest) {
        for (i in 0 until request.antall) {
            journalPostService.lagJournalPost(request.forVirksomhet)
                .let {
                    val journalpostMap = journalpostApi.opprettJournalpost(it, false)
                    opprettJfrOppgave(it, journalpostMap["journalpostId"] as String, request.tilordnetRessurs)
                }
        }
    }

    private fun opprettJfrOppgave(request: OpprettJournalpostRequest, journalpostID: String, tilordnetRessurs: String) {
        oppgaveApi.opprettOppgave(
            Oppgave(
                aktoerId = if (request.bruker?.idType === BrukerIdType.FNR) PersonRepo.finnVedIdent(
                    request.bruker.id ?: ""
                )?.aktørId else null,
                orgnr = if (request.bruker?.idType === BrukerIdType.ORGNR) request.bruker.id else null,
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

    data class OpprettJfrOppgaveRequest(
        val antall: Int,
        val tilordnetRessurs: String,
        val forVirksomhet: Boolean = false
    )
}
