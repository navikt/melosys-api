package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.brev.VedtakOpphoertMedlemskapBrevbestilling
import no.nav.melosys.domain.kodeverk.Mottakerroller
import java.time.LocalDate

class VedtakOpphoertMedlemskap(
    brevbestilling: VedtakOpphoertMedlemskapBrevbestilling,

    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val datoMottatt: LocalDate?,

    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val opphoertDato: LocalDate?,

    val fritekst: String?,
) : DokgenDto(brevbestilling, Mottakerroller.BRUKER) {

    constructor(brevbestilling: VedtakOpphoertMedlemskapBrevbestilling) : this(
        brevbestilling = brevbestilling,
        datoMottatt = instantTilLocalDate(brevbestilling.forsendelseMottatt),
        opphoertDato = brevbestilling.opphørtDato,
        fritekst = brevbestilling.opphørtBegrunnelseFritekst
    )
}
