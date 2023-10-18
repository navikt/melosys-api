package no.nav.melosys.service.lovvalgsperiode

import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

data class OpprettLovvalgsperiodeRequest(
    val fomDato: LocalDate?,
    val tomDato: LocalDate?,
    val lovvalgsbestemmelse: LovvalgBestemmelse?,
    val innvilgelsesResultat: InnvilgelsesResultat?,
    val trygdedekning: Trygdedekninger?,
)
