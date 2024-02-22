package no.nav.melosys.tjenester.gui.avklartefakta

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.tjenester.gui.dto.oppsummertefakta.ArbeidslandDto
import no.nav.melosys.tjenester.gui.dto.oppsummertefakta.VirksomheterDto

data class AvklartefaktaOppsummeringDto private constructor(
    val virksomheter: VirksomheterDto,
    val arbeidsland: ArbeidslandDto,
    val familieRelasjonType: String?,
    val fullstendigManglendeInnbetaling: Boolean?,
    val oppholdType: String?,
) {
    constructor(avklartefakta: Set<AvklartefaktaDto>) : this(
        virksomheter = VirksomheterDto.av(avklartefakta),
        arbeidsland = ArbeidslandDto.av(avklartefakta),
        familieRelasjonType = hentFamileRelasjonType(avklartefakta),
        fullstendigManglendeInnbetaling = hentFullstendigManglendeInnbetaling(avklartefakta),
        oppholdType = hentOppholdType(avklartefakta)
    )

    companion object {
        private fun hentFamileRelasjonType(avklartefakta: Set<AvklartefaktaDto>): String? = avklartefakta.firstOrNull {
            it.avklartefaktaType == Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON
        }?.fakta?.firstOrNull()

        fun hentFullstendigManglendeInnbetaling(avklartefakta: Set<AvklartefaktaDto>): Boolean? = avklartefakta.firstOrNull {
            it.avklartefaktaType == Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING
        }?.fakta?.firstOrNull().toBoolean()

        private fun hentOppholdType(avklartefakta: Set<AvklartefaktaDto>): String? = avklartefakta.firstOrNull {
            it.avklartefaktaType == Avklartefaktatyper.IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD
        }?.fakta?.firstOrNull()
    }
}
