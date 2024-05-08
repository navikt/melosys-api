package no.nav.melosys.service.sak

import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.integrasjon.trygdeavgift.dto.DatoPeriodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.PengerDto
import java.math.BigDecimal

data class TrygdeavgiftsperiodeDto(
    val periode: DatoPeriodeDto,
    val sats: BigDecimal,
    val månedsavgift: PengerDto,
    val inntektsKilde: Inntektskildetype,
    val bruttoInntekt: BigDecimal,
    val arbeidsGiveravgiftBetalesTilSkatt: Boolean
)
