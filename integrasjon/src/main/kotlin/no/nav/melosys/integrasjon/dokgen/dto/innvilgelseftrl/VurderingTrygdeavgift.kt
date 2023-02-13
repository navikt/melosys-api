package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class VurderingTrygdeavgift(
    val norsk: TrygdeavgiftInfo?,
    val utenlandsk: TrygdeavgiftInfo?,
    val selvbetalende: Boolean,
    val representantNavn: String?
)
