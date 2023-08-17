package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.service.lovvalgsperiode.OpprettLovvalgsperiodeRequest
import java.time.LocalDate

data class OpprettLovvalgsperiodeDto(
    val fomDato: LocalDate?,
    val tomDato: LocalDate?,
    val lovvalgsbestemmelse: String?,
    val innvilgelsesResultat: InnvilgelsesResultat?
) {
    fun tilRequest(): OpprettLovvalgsperiodeRequest {
        return OpprettLovvalgsperiodeRequest(
            fomDato,
            tomDato,
            if (lovvalgsbestemmelse.isNullOrEmpty()) null
            else LovvalgBestemmelsekonverterer().convertToEntityAttribute(lovvalgsbestemmelse),
            innvilgelsesResultat
        )
    }
}
