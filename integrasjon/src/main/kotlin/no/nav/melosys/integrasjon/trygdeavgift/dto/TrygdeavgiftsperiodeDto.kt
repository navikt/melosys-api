package no.nav.melosys.integrasjon.trygdeavgift.dto

data class TrygdeavgiftsperiodeDto(
    val periode: DatoPeriodeDto,
    val sats: Double,
    val avgift: PengerDto,
    var grunnlagInntektsperiode: String,
    var grunnlagMedlemskapsperiode: String,
    var grunnlagSkatteforholdsperiode: String,
)
