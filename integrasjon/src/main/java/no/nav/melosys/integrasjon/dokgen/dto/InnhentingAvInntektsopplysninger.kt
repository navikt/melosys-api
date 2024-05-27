package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.brev.InnhentingAvInntektsopplysningerBrevbestilling
import no.nav.melosys.domain.kodeverk.Mottakerroller
import java.time.LocalDate

class InnhentingAvInntektsopplysninger(
    brevbestilling: InnhentingAvInntektsopplysningerBrevbestilling,

    val årsavregningsår: Int,

    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val fristdato: LocalDate,

    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val medlemskapsperiodeFom: LocalDate,

    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val medlemskapsperiodeTom: LocalDate,

    val skalViseStandardTekstOmOpplysninger: Boolean,

    val fritekst: String?,
) : DokgenDto(brevbestilling, Mottakerroller.BRUKER) {

    constructor(
        brevbestilling: InnhentingAvInntektsopplysningerBrevbestilling,
        årsavregningsår: Int,
        fristdato: LocalDate,
        medlemskapsperiodeFom: LocalDate,
        medlemskapsperiodeTom: LocalDate
    ) : this(
        brevbestilling,
        årsavregningsår,
        fristdato,
        medlemskapsperiodeFom,
        medlemskapsperiodeTom,
        brevbestilling.skalViseStandardTekstOmOpplysninger,
        brevbestilling.fritekst
    )
}
