package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.BeregnetTrygdeavgiftDto
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.InntekskildeDto
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.SkatteforholdTilNorgeDto

data class ÅrsavregningDto(
    val beregnetTrygdeavgiftDto: BeregnetTrygdeavgiftDto,
    val skatteforholdsperioder: List<SkatteforholdTilNorgeDto>,
    val inntektskilder: List<InntekskildeDto>,
    val medlemskapsperioder: List<MedlemskapsperiodeDto>,
)
