package no.nav.melosys.service.sak

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.DatoPeriodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.PengerDto


data class ÅrsavregningDto(
    val trygdeavgiftsPerioder: List<TrygdeavgiftsperiodeDto>,
    val skatteforholdsperioder: List<SkatteforholdTilNorgeDto>,
    val medlemskapsperioder: List<MedlemskapsperiodeDto>,
) {
    companion object {
        fun av(trygdeavgiftsPerioder: List<Trygdeavgiftsperiode>,
               skatteforholdsperioder: List<SkatteforholdTilNorge>,
               medlemskapsperioder: List<Medlemskapsperiode>): ÅrsavregningDto {

            //TODO vi må også filtrere basert på sluttdato
            val trygdeavgiftsPerioderDto = trygdeavgiftsPerioder.map {
                TrygdeavgiftsperiodeDto(periode = DatoPeriodeDto(it.periodeFra, it.periodeTil),
                    sats = it.trygdesats,
                    månedsavgift = PengerDto(it.trygdeavgiftsbeløpMd),
                    arbeidsGiveravgiftBetalesTilSkatt = it.grunnlagInntekstperiode.isArbeidsgiversavgiftBetalesTilSkatt,
                    bruttoInntekt = it.grunnlagInntekstperiode.avgiftspliktigInntektMnd.verdi,
                    inntektsKilde = it.grunnlagInntekstperiode.type
                )
            }.sortedWith(compareBy { it.periode.fom })

            val skatteforholdsPerioderDto = skatteforholdsperioder.map {
                SkatteforholdTilNorgeDto(periode = DatoPeriodeDto(it.fomDato, it.tomDato), skatteforhold = it.skatteplikttype)
            }.sortedWith(compareBy { it.periode.fom })

            val medlemskapsperioderDto = medlemskapsperioder.map { MedlemskapsperiodeDto(it) }.sortedWith(compareBy { it.fom })
            return ÅrsavregningDto(trygdeavgiftsPerioderDto, skatteforholdsPerioderDto, medlemskapsperioderDto)
        }
    }
}


