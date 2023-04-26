package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import java.math.BigInteger

data class Inntektsperiode(
    val periode: DatoPeriode,
    val inntektskilde: Inntektskildetype,
    val skatteplikt: Skatteplikttype,
    val arbeidsgiverBetalerAvgift: Boolean,
    val trygdeavgiftBetalesTilSkatt: Boolean,
    val månedsbeløp: Penger?
)

data class Penger(val verdi: BigInteger, var valuta: Valuta = NOK)

data class Valuta(val kode: String, val desimaler: Int = 2)

val NOK = Valuta("NOK", 2)
