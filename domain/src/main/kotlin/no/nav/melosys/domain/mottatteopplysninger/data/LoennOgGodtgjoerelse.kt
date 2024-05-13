package no.nav.melosys.domain.mottatteopplysninger.data

import java.math.BigDecimal

data class LoennOgGodtgjoerelse(
    var norskArbgUtbetalerLoenn: Boolean? = null,
    var erArbeidstakerAnsattHelePerioden: Boolean? = null,
    var utlArbgUtbetalerLoenn: Boolean? = null,
    var utlArbTilhoererSammeKonsern: Boolean? = null,
    var bruttoLoennPerMnd: BigDecimal? = null,
    var bruttoLoennUtlandPerMnd: BigDecimal? = null,
    var mottarNaturalytelser: Boolean? = null,
    var samletVerdiNaturalytelser: BigDecimal? = null,
    var erArbeidsgiveravgiftHelePerioden: Boolean? = null,
    var erTrukketTrygdeavgift: Boolean? = null
)

