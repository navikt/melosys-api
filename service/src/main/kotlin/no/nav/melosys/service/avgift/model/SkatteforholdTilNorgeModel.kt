package no.nav.melosys.service.avgift.model

import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import java.time.LocalDate

data class SkatteforholdTilNorgeModel(
    val fomDato: LocalDate,
    val tomDato: LocalDate?,
    val skatteplikttype: Skatteplikttype
) {
    constructor(entity: SkatteforholdTilNorge, justertFomPeriode: LocalDate? = null) : this(
        fomDato = justertFomPeriode?.let { if (entity.fomDato.isBefore(it)) it else entity.fomDato } ?: entity.fomDato,
        tomDato = entity.tomDato,
        skatteplikttype = entity.skatteplikttype
    )
}
