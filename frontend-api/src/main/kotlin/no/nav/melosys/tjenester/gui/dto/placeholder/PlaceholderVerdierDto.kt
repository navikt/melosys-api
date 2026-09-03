package no.nav.melosys.tjenester.gui.dto.placeholder

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.service.placeholder.BetingelseVerdi
import no.nav.melosys.service.placeholder.PlaceholderVerdi
import no.nav.melosys.service.placeholder.PlaceholderVerdier

data class PlaceholderVerdierDto(
    val verdier: List<PlaceholderVerdiDto>,
    // Alltid med, men bare med de betingelsene som lot seg vurdere for behandlingen
    val betingelser: List<BetingelseVerdiDto>,
) {
    companion object {
        fun av(verdier: PlaceholderVerdier): PlaceholderVerdierDto = PlaceholderVerdierDto(
            verdier = verdier.verdier.map(PlaceholderVerdiDto::av),
            betingelser = verdier.betingelser.map(BetingelseVerdiDto::av),
        )
    }
}

data class BetingelseVerdiDto(
    val nokkel: String,
    val oppfylt: Boolean,
) {
    companion object {
        fun av(betingelse: BetingelseVerdi): BetingelseVerdiDto =
            BetingelseVerdiDto(nokkel = betingelse.nokkel, oppfylt = betingelse.oppfylt)
    }
}

data class PlaceholderVerdiDto(
    val nokkel: String,
    val verdi: String,
    // Kontrakten krever at feltet er helt fraværende når verdien ikke er flertydig
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val kandidater: List<String>? = null,
) {
    companion object {
        fun av(verdi: PlaceholderVerdi): PlaceholderVerdiDto = PlaceholderVerdiDto(
            nokkel = verdi.nokkel,
            verdi = verdi.verdi,
            kandidater = verdi.kandidater,
        )
    }
}
