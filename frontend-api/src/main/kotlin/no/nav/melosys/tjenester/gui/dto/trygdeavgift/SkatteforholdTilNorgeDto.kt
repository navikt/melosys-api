package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.service.avgift.model.SkatteforholdTilNorgeModel
import java.time.LocalDate

data class SkatteforholdTilNorgeDto(
    val fomDato: LocalDate,
    val tomDato: LocalDate?,
    val skatteplikttype: Skatteplikttype,
) {
    constructor(skatteforholdTilNorge: SkatteforholdTilNorge) : this(
        skatteforholdTilNorge.fomDato,
        skatteforholdTilNorge.tomDato,
        skatteforholdTilNorge.skatteplikttype,
    )

    constructor(model: SkatteforholdTilNorgeModel) : this(
        model.fomDato,
        model.tomDato,
        model.skatteplikttype
    )
}
