package no.nav.melosys.tjenester.gui.dto.trygdeavgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlaget
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest

data class TrygdeavgiftsgrunnlagDto(
    val skatteplikttype: Skatteplikttype,
    val inntektskilder: Set<InntekskildeDto>
) {
    fun tilRequest(): OppdaterTrygdeavgiftsgrunnlagRequest =
        OppdaterTrygdeavgiftsgrunnlagRequest(skatteplikttype, (inntektskilder.map { it.tilRequest() }).toSet())

    companion object {
        @JvmStatic
        fun av(trygdeavgiftsgrunnlaget: Trygdeavgiftsgrunnlaget): TrygdeavgiftsgrunnlagDto =
            TrygdeavgiftsgrunnlagDto(
                trygdeavgiftsgrunnlaget.skatteforholdTilNorge.first().skatteplikttype,
                (trygdeavgiftsgrunnlaget.inntektskilder.map { InntekskildeDto.av(it) }).toSet())
    }
}
