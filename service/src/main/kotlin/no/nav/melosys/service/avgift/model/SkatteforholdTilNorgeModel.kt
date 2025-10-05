package no.nav.melosys.service.avgift.model

import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import java.time.LocalDate

data class SkatteforholdTilNorgeModel(
    val fomDato: LocalDate,
    val tomDato: LocalDate?,
    val skatteplikttype: Skatteplikttype
) {
    companion object {
        fun fromEntity(skatteforhold: SkatteforholdTilNorge): SkatteforholdTilNorgeModel {
            return SkatteforholdTilNorgeModel(
                fomDato = skatteforhold.fomDato,
                tomDato = skatteforhold.tomDato,
                skatteplikttype = skatteforhold.skatteplikttype
            )
        }
    }
}
