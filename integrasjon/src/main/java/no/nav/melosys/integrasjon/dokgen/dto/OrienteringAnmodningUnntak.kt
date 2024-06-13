package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.brev.OrienteringAnmodningUnntakBrevbestilling
import no.nav.melosys.domain.kodeverk.Mottakerroller
import java.time.LocalDate

class OrienteringAnmodningUnntak(
    brevbestilling: OrienteringAnmodningUnntakBrevbestilling,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val periodeFom: LocalDate,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val periodeTom: LocalDate,
    val arbeidsland: String,
    val erDirekteTilAnmodningOmUnntak: Boolean,
    val erAnmodningOmUnntakViaArbeidstaker: Boolean,
    val erAnmodningOmUnntakViaNæringsdrivende: Boolean,
    val lovvalgsbestemmelse: String,
    val begrunnelser: List<String>,
    val direkteTilAnmodningBegrunnelser: List<String>,
    val anmodningBegrunnelser: List<String>,
    val fritekst: String?
) : DokgenDto(brevbestilling, Mottakerroller.BRUKER) {

    constructor(
        brevbestilling: OrienteringAnmodningUnntakBrevbestilling,
        periodeFom: LocalDate,
        periodeTom: LocalDate,
        arbeidsland: String,
        erDirekteTilAnmodningOmUnntak: Boolean,
        erAnmodningOmUnntakViaArbeidstaker: Boolean,
        erAnmodningOmUnntakViaNæringsdrivende: Boolean,
        lovvalgsbestemmelse: String,
        begrunnelser: List<String>,
        direkteTilAnmodningBegrunnelser: List<String>,
        anmodningBegrunnelser: List<String>,
    ) : this(
        brevbestilling,
        periodeFom,
        periodeTom,
        arbeidsland,
        erDirekteTilAnmodningOmUnntak,
        erAnmodningOmUnntakViaArbeidstaker,
        erAnmodningOmUnntakViaNæringsdrivende,
        lovvalgsbestemmelse,
        begrunnelser,
        direkteTilAnmodningBegrunnelser,
        anmodningBegrunnelser,
        brevbestilling.anmodningUnntakFritekst
    )
}
