package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import java.time.LocalDate

data class SkatteforholdTilNorgeDto(
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val skatteplikttype: Skatteplikttype,
) {
    constructor(skatteforholdTilNorge: SkatteforholdTilNorge) : this(
        skatteforholdTilNorge.fomDato,
        skatteforholdTilNorge.tomDato,
        skatteforholdTilNorge.skatteplikttype,
    )

    companion object {
        fun List<SkatteforholdTilNorgeDto>.tilSkatteforholdsPerioder(): List<SkatteforholdTilNorge> = map {
            SkatteforholdTilNorge().apply {
                fomDato = it.fomDato
                tomDato = it.tomDato
                skatteplikttype = it.skatteplikttype
            }
        }
    }
}
