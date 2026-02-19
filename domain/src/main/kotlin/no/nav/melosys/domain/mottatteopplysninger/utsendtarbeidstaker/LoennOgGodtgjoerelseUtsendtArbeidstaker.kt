package no.nav.melosys.domain.mottatteopplysninger.utsendtarbeidstaker

import java.math.BigDecimal

data class LoennOgGodtgjoerelseUtsendtArbeidstaker(
    val norskArbgUtbetalerLoenn: Boolean? = null,
    val erArbeidstakerAnsattHelePerioden: Boolean? = null,
    val utlArbgUtbetalerLoenn: Boolean? = null,
    val utlArbTilhoererSammeKonsern: Boolean? = null,
    val bruttoLoennPerMnd: BigDecimal? = null,
    val bruttoLoennUtlandPerMnd: BigDecimal? = null,
    val mottarNaturalytelser: Boolean? = null,
    val samletVerdiNaturalytelser: BigDecimal? = null,
    val erArbeidsgiveravgiftHelePerioden: Boolean? = null,
    val erTrukketTrygdeavgift: Boolean? = null
)
