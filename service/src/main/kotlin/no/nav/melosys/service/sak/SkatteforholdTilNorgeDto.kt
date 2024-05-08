package no.nav.melosys.service.sak

import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.integrasjon.trygdeavgift.dto.DatoPeriodeDto

data class SkatteforholdTilNorgeDto (
    val periode: DatoPeriodeDto,
    val skatteforhold: Skatteplikttype
)
