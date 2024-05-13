package no.nav.melosys.saksflytapi.journalfoering

import no.nav.melosys.domain.kodeverk.Avsendertyper
import no.nav.melosys.domain.kodeverk.ForvaltningsmeldingMottaker
import java.time.LocalDate

interface JournalfoeringRequest {
    val journalpostID: String?
    val vedlegg: List<DokumentRequest>?
    val mottattDato: LocalDate?
    val skalTilordnes: Boolean?
    val avsenderNavn: String?
    val avsenderID: String?
    val avsenderType: Avsendertyper?
    val virksomhetOrgnr: String?
    val brukerID: String?
    val oppgaveID: String?
    val hoveddokument: DokumentRequest?
    val forvaltningsmeldingMottaker: ForvaltningsmeldingMottaker?
}
