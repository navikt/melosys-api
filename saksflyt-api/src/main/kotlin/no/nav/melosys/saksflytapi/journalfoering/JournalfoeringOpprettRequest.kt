package no.nav.melosys.saksflytapi.journalfoering

import no.nav.melosys.domain.kodeverk.Avsendertyper
import no.nav.melosys.domain.kodeverk.ForvaltningsmeldingMottaker
import java.time.LocalDate

data class JournalfoeringOpprettRequest(
    override var journalpostID: String? = null,
    override var oppgaveID: String? = null,
    override var brukerID: String? = null,
    override var virksomhetOrgnr: String? = null,
    override var avsenderID: String? = null,
    override var avsenderNavn: String? = null,
    override var avsenderType: Avsendertyper? = null,
    override var hoveddokument: DokumentRequest? = null,
    override var vedlegg: List<DokumentRequest> = emptyList(),
    override var skalTilordnes: Boolean = false,
    override var mottattDato: LocalDate? = null,
    override var forvaltningsmeldingMottaker: ForvaltningsmeldingMottaker? = null,
    var behandlingstemaKode: String? = null,
    var behandlingstypeKode: String? = null,
    val fagsak: Fagsak? = null,
    val arbeidsgiverID: String? = null,
) : JournalfoeringRequest {
}

data class Fagsak(
    var sakstema: String? = null,
    var sakstype: String? = null,
    var soknadsperiode: Periode? = null,
    var land: Soeknadsland? = null,
)


data class Periode(
    var fom: LocalDate? = null,
    var tom: LocalDate? = null,
)

data class Soeknadsland(
    var landkoder: List<String>? = null,
    var erUkjenteEllerAlleEosLand: Boolean = false,
)
