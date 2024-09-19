package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.brev.ÅrsavregningVedtakBrevBestilling
import no.nav.melosys.domain.kodeverk.Mottakerroller
import java.time.LocalDate

class ÅrsavregningVedtaksbrev(
    brevBestilling: ÅrsavregningVedtakBrevBestilling,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val datoMottatt: LocalDate?,
    val innledningFritekst: String?,
    val begrunnelseFritekst: String?,
) : DokgenDto(brevBestilling, Mottakerroller.BRUKER) {
    constructor(
        brevBestilling: ÅrsavregningVedtakBrevBestilling,
    ) : this(
        brevBestilling = brevBestilling,
        datoMottatt = instantTilLocalDate(brevBestilling.forsendelseMottatt),
        innledningFritekst = brevBestilling.innledningFritekstAarsavregning,
        begrunnelseFritekst = brevBestilling.begrunnelseFritekstAarsavregning
    )
}
