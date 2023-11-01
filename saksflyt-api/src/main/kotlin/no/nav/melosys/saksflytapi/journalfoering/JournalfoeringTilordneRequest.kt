package no.nav.melosys.saksflytapi.journalfoering

import no.nav.melosys.domain.kodeverk.Avsendertyper
import java.time.LocalDate

data class JournalfoeringTilordneRequest(
    override var journalpostID: String? = null,
    override var oppgaveID: String? = null,
    override var brukerID: String? = null,
    override var virksomhetOrgnr: String? = null,
    override var avsenderID: String? = null,
    override var avsenderNavn: String? = null,
    override var avsenderType: Avsendertyper? = null,
    override var hoveddokument: DokumentRequest? = null,
    override var vedlegg: List<DokumentRequest> = emptyList(),
    override var mottattDato: LocalDate? = null,
    override var skalTilordnes: Boolean = false,
    override var ikkeSendForvaltingsmelding: Boolean? = null,
    var behandlingstemaKode: String? = null,
    var behandlingstypeKode: String? = null,
    var saksnummer: String? = null,
    val ingenVurdering: Boolean = false,
) : JournalfoeringRequest
