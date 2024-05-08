package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.InntekskildeDto
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.SkatteforholdTilNorgeDto
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.TrygdeavgiftsperiodeDto

data class ÅrsavregningDto(
    val trygdeavgiftsPerioder: List<TrygdeavgiftsperiodeDto>,
    val skatteforholdsperioder: List<SkatteforholdTilNorgeDto>,
    val inntektskilder: List<InntekskildeDto>,
    val medlemskapsperioder: List<MedlemskapsperiodeDto>,
) {
    companion object {
        fun av(trygdeavgiftsPerioder: List<Trygdeavgiftsperiode>,
               skatteforholdsperioder: List<SkatteforholdTilNorge>,
               inntektskilder: List<Inntektsperiode>,
               medlemskapsperioder: List<Medlemskapsperiode>): ÅrsavregningDto {

            //TODO vi må også filtrere basert på sluttdato
            val trygdeavgiftsPerioderDto = trygdeavgiftsPerioder.map { TrygdeavgiftsperiodeDto(it) }.sortedWith(compareBy { it.fom })
            val skatteforholdsPerioderDto = skatteforholdsperioder.map { SkatteforholdTilNorgeDto(it) }.sortedWith(compareBy { it.fomDato })
            val inntektskilderDto = inntektskilder.map { InntekskildeDto(it) }.sortedWith(compareBy { it.fomDato })
            val medlemskapsperioderDto = medlemskapsperioder.map { MedlemskapsperiodeDto(it) }.sortedWith(compareBy { it.fom })

            return ÅrsavregningDto(trygdeavgiftsPerioderDto, skatteforholdsPerioderDto, inntektskilderDto, medlemskapsperioderDto)
        }
    }
}


