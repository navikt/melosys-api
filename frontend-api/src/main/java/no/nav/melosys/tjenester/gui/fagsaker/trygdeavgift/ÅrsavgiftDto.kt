package no.nav.melosys.tjenester.gui.fagsaker.trygdeavgift

import no.nav.melosys.integrasjon.trygdeavgift.dto.InntektsperiodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.MedlemskapsperiodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.SkatteforholdsperiodeDto

data class ÅrsavgiftDto (
    val skatteforholdsperioder: List<SkatteforholdsperiodeDto>,
    val inntektskilder: List<InntektsperiodeDto>,
    val medlemskapsPerioder: List<MedlemskapsperiodeDto>,
    val år: Int,
)

