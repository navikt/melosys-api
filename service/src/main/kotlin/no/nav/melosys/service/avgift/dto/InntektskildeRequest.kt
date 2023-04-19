package no.nav.melosys.service.avgift.dto

import no.nav.melosys.domain.kodeverk.Inntektskildetype
import java.math.BigInteger

class InntektskildeRequest(
    val type: Inntektskildetype,
    val avgiftspliktigInntektMnd: BigInteger,
    val arbeidsgiversavgiftBetales: Boolean
)
