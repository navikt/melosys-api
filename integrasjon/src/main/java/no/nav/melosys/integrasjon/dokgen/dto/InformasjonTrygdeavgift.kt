package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.kodeverk.Betalingstype
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
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
    val trygdeavgiftMottaker: Trygdeavgiftmottaker,
    val erNordisk: Boolean,
    val betalingsvalg: Betalingstype,
    val fullmektigTrygdeavgift: String?,
    val avgiftsperioder: List<Avgiftsperiode>,
    val innevaerendeAar: Int,
    val lavSatsInnevaerendeAar: Double,
    val mellomSatsInnevaerendeAar: Double
) : DokgenDto(brevbestilling, Mottakerroller.BRUKER) {

}
