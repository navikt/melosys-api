package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.brev.OrienteringTilArbeidsgiverOmVedtakBrevbestilling
import no.nav.melosys.domain.kodeverk.Mottakerroller
import java.time.LocalDate

class OrienteringTilArbeidsgiverOmVedtak(
    brevbestilling: OrienteringTilArbeidsgiverOmVedtakBrevbestilling,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val periodeFom: LocalDate,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val periodeTom: LocalDate,
    val arbeidsland: String,
    val erInnvilgelse: Boolean,
    val erVesentligVirksomhetOppfyllt: Boolean,
    val navnVirksomhet: String,
    val vesentligVirksomhetBegrunnelser: List<String>
) : DokgenDto(brevbestilling, Mottakerroller.ARBEIDSGIVER) {
}
