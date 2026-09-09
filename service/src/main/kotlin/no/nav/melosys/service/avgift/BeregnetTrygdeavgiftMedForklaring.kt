package no.nav.melosys.service.avgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.integrasjon.trygdeavgift.dto.BeregningsforklaringDto

/**
 * Resultatet av en trygdeavgiftsberegning på PUT-veien: de persisterte periodene samt de
 * distinkte beregningsforklaringene fra beregningsmotoren. Forklaringene persisteres ikke.
 */
data class BeregnetTrygdeavgiftMedForklaring(
    val trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>,
    val beregningsforklaringer: List<BeregningsforklaringDto>,
)
