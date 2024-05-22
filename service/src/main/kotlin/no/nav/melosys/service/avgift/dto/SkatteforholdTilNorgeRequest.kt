package no.nav.melosys.service.avgift.dto

import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import java.time.LocalDate
import java.util.UUID

data class SkatteforholdTilNorgeRequest(
    val fomDato: LocalDate,
    val tomDato: LocalDate?,
    val skatteplikttype: Skatteplikttype
) {
    constructor(skatteforholdTilNorge: SkatteforholdTilNorge) : this(
        skatteforholdTilNorge.fomDato,
        skatteforholdTilNorge.tomDato,
        skatteforholdTilNorge.skatteplikttype
    )
}
