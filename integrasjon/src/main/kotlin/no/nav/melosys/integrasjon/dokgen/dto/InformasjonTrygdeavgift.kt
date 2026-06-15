package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.kodeverk.Betalingstype
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer
import java.math.BigDecimal
import java.time.LocalDate

class InformasjonTrygdeavgift(
    brevbestilling: DokgenBrevbestilling,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val fomDato: LocalDate,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val tomDato: LocalDate,
    val bostedLand: String,
    val begrunnelseFritekst: String?,
    val trygdeavgiftMottaker: Trygdeavgiftmottaker?,
    val erNordisk: Boolean,
    val betalingsvalg: Betalingstype,
    val fullmektigTrygdeavgift: String?,
    val avgiftsperioder: List<AvgiftsperiodeEøsPensjonist>,
    val harAvgiftspliktigePerioderIForegåendeÅr: Boolean,
    val erSkattemessigEmigrert: Boolean,
    val minstebelopVerdi: BigDecimal? = null,
    val minstebelopAar: Int? = null
) : DokgenDto(brevbestilling, Mottakerroller.BRUKER) {

}

data class AvgiftsperiodeEøsPensjonist(
    val fom: LocalDate,
    val tom: LocalDate,
    val avgiftssats: BigDecimal?,
    val avgiftPerMd: BigDecimal,
    val avgiftspliktigInntektPerMd: BigDecimal,
    val inntektskilde: String,
    val skatteplikt: Boolean,
    val beregningsregel: Avgiftsberegningsregel
)
