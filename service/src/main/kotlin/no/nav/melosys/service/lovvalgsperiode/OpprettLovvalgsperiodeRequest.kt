package no.nav.melosys.service.lovvalgsperiode

import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import java.time.LocalDate

data class OpprettLovvalgsperiodeRequest(
    val fomDato: LocalDate?,
    val tomDato: LocalDate?,
    val lovvalgsbestemmelse: LovvalgBestemmelse?,
    val innvilgelsesResultat: InnvilgelsesResultat?
)
