package no.nav.melosys.melosysmock.testdata

import no.nav.melosys.melosysmock.journalpost.journalpostapi.*
import no.nav.melosys.melosysmock.organisasjon.OrganisasjonRepo
import no.nav.melosys.melosysmock.person.PersonRepo
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class JournalPostService {
    fun lagJournalPost(forVirksomhet: Boolean = false): OpprettJournalpostRequest =
        OpprettJournalpostRequest(
            journalpostType = JournalpostType.INNGAAENDE,
            avsenderMottaker = lagAvsenderMottaker(forVirksomhet),
            bruker = lagBruker(forVirksomhet),
            datoMottatt = LocalDate.now(),
            tema = "MED",
            tittel = "Søknad om A1 for uatsendte arbeidstakere i EØS/Sveits",
            kanal = "SKAN_NETS",
            journalfoerendeEnhet = "4530",
            dokumenter = listOf(
                Dokument(
                    tittel = "Tittel til Dokument",
                    brevkode = null,
                    dokumentKategori = "SOK",
                    dokumentvarianter = listOf(
                        DokumentVariant(
                            filtype = JournalpostFiltype.PDFA,
                            variantformat = "ARKIV",
                            fysiskDokument = JournalPostService::class.java.getResource("/dummy.pdf").readBytes()
                        )
                    )
                )
            )
        )

    private fun lagAvsenderMottaker(forVirksomhet: Boolean = false): AvsenderMottaker =
        if (forVirksomhet) {
            AvsenderMottaker(id = OrganisasjonRepo.repo.values.first().orgnr, idType = IdType.ORGNR)
        } else {
            AvsenderMottaker(id = PersonRepo.repo.values.first().ident, idType = IdType.FNR)
        }

    private fun lagBruker(forVirksomhet: Boolean = false): Bruker =
        if (forVirksomhet) {
            Bruker(idType = BrukerIdType.ORGNR, id = OrganisasjonRepo.repo.values.first().orgnr)
        } else {
            Bruker(idType = BrukerIdType.FNR, id = PersonRepo.repo.values.first().ident)
        }
}
