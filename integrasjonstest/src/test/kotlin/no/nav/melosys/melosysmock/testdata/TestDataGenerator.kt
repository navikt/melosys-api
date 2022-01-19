package no.nav.melosys.melosysmock.testdata

import no.nav.melosys.melosysmock.journalpost.journalpostapi.*
import no.nav.melosys.melosysmock.journalpost.journalpostapi.JournalpostApi
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
class TestDataGenerator {

    @PostMapping("/jfr-oppgave")
    fun lagJournalføringsoppgave(@RequestBody request: OpprettJfrOppgaveRequest) {
            lagJournalpost()
                .let { JournalpostApi().opprettJournalpost(it, false, request.journalPostId) }
                .also { opprettJfrOppgave(request.journalPostId, request.tilordnetRessurs) }
    }

    private fun opprettJfrOppgave(journalpostID: String, tilordnetRessurs: String) {
        OppgaveApi().opprettOppgave(
            Oppgave(
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

    private fun lagJournalpost(): OpprettJournalpostRequest = OpprettJournalpostRequest(
        journalpostType = JournalpostType.INNGAAENDE,
        avsenderMottaker = AvsenderMottaker(id = PersonRepo.repo.values.first().ident, idType = IdType.FNR),
        bruker = Bruker(idType = BrukerIdType.FNR, id = PersonRepo.repo.values.first().ident),
        datoMottatt = LocalDate.now(),
        tema = "MED",
        tittel = "Søknad om A1 for utsendte arbeidstakere i EØS/Sveits",
        kanal = "SKAN_NETS",
        journalfoerendeEnhet = "4530",
        dokumenter = listOf(
            Dokument(
                tittel = "Søknad om A1 for utsendte arbeidstakere i EØS/Sveits",
                brevkode = null,
                dokumentKategori = "SOK",
                dokumentvarianter = listOf(
                    DokumentVariant(
                        filtype = JournalpostFiltype.PDFA,
                        variantformat = "ARKIV",
                        fysiskDokument = TestDataGenerator::class.java.getResource("/dummy.pdf").readBytes()
                    )
                )
            )
        )
    )
}


data class OpprettJfrOppgaveRequest(
    val tilordnetRessurs: String,
    val journalPostId: String
)
