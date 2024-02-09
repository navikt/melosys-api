package no.nav.melosys.domain.mottatteopplysninger.data

import java.math.BigDecimal


class LoennOgGodtgjoerelse {
    var norskArbgUtbetalerLoenn: Boolean? = null
    var erArbeidstakerAnsattHelePerioden: Boolean? = null
    var utlArbgUtbetalerLoenn: Boolean? = null
    var utlArbTilhoererSammeKonsern: Boolean? = null
    var bruttoLoennPerMnd: BigDecimal? = null
    var bruttoLoennUtlandPerMnd: BigDecimal? = null
    var mottarNaturalytelser: Boolean? = null
    var samletVerdiNaturalytelser: BigDecimal? = null
    var erArbeidsgiveravgiftHelePerioden: Boolean? = null
    var erTrukketTrygdeavgift: Boolean? = null

    constructor()
    constructor(
        norskArbgUtbetalerLoenn: Boolean?, erArbeidstakerAnsattHelePerioden: Boolean?,
        utlArbgUtbetalerLoenn: Boolean?, utlArbTilhoererSammeKonsern: Boolean?,
        bruttoLoennPerMnd: BigDecimal?, bruttoLoennUtlandPerMnd: BigDecimal?,
        mottarNaturalytelser: Boolean?, samletVerdiNaturalytelser: BigDecimal?,
        erArbeidsgiveravgiftHelePerioden: Boolean?, erTrukketTrygdeavgift: Boolean?
    ) {
        this.norskArbgUtbetalerLoenn = norskArbgUtbetalerLoenn
        this.erArbeidstakerAnsattHelePerioden = erArbeidstakerAnsattHelePerioden
        this.utlArbgUtbetalerLoenn = utlArbgUtbetalerLoenn
        this.utlArbTilhoererSammeKonsern = utlArbTilhoererSammeKonsern
        this.bruttoLoennPerMnd = bruttoLoennPerMnd
        this.bruttoLoennUtlandPerMnd = bruttoLoennUtlandPerMnd
        this.mottarNaturalytelser = mottarNaturalytelser
        this.samletVerdiNaturalytelser = samletVerdiNaturalytelser
        this.erArbeidsgiveravgiftHelePerioden = erArbeidsgiveravgiftHelePerioden
        this.erTrukketTrygdeavgift = erTrukketTrygdeavgift
    }
}
