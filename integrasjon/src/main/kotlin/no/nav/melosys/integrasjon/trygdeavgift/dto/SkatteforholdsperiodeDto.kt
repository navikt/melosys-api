package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import java.util.*

data class SkatteforholdsperiodeDto(
    val id: UUID,
    val periode: DatoPeriodeDto,
    val skatteforhold: Skatteplikttype
) {
    companion object {
        fun tilSkatteforhold(skatteforholdTilNorgeRequest: SkatteforholdsperiodeDto) = SkatteforholdTilNorge().apply {
            this.fomDato = skatteforholdTilNorgeRequest.periode.fom
            this.tomDato = skatteforholdTilNorgeRequest.periode.tom
            this.skatteplikttype = skatteforholdTilNorgeRequest.skatteforhold
        }
    }
}
