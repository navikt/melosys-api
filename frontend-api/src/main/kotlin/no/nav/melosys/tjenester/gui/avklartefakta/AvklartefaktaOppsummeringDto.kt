package no.nav.melosys.tjenester.gui.avklartefakta

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.tjenester.gui.dto.oppsummertefakta.ArbeidslandDto
import no.nav.melosys.tjenester.gui.dto.oppsummertefakta.VirksomheterDto

data class AvklartefaktaOppsummeringDto internal constructor(
    val virksomheter: VirksomheterDto,
    val arbeidsland: ArbeidslandDto,
    val fullstendigManglendeInnbetaling: Boolean?,
    val ikkeYrkesaktivFamilieRelasjonstype: String?,
    val ikkeYrkesaktivOppholdstype: String?,
    val arbeidssituasjonType: String?,
    val ukjentSluttdato: Boolean?
) {
    constructor(avklartefakta: Set<AvklartefaktaDto>) : this(
        virksomheter = VirksomheterDto.av(avklartefakta),
        arbeidsland = ArbeidslandDto.av(avklartefakta),
        fullstendigManglendeInnbetaling = hentFullstendigManglendeInnbetaling(avklartefakta),
        ikkeYrkesaktivFamilieRelasjonstype = hentFamileRelasjonType(avklartefakta),
        ikkeYrkesaktivOppholdstype = hentOppholdType(avklartefakta),
        arbeidssituasjonType = hentArbeidssituasjonType(avklartefakta),
        ukjentSluttdato = hentUkjentSluttdato(avklartefakta)
    )

    companion object {
        private fun hentFamileRelasjonType(avklartefakta: Set<AvklartefaktaDto>): String? = avklartefakta.firstOrNull {
            it.avklartefaktaType == Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON
        }?.fakta?.firstOrNull()

        private fun hentArbeidssituasjonType(avklartefakta: Set<AvklartefaktaDto>): String? = avklartefakta.firstOrNull {
            it.avklartefaktaType == Avklartefaktatyper.ARBEIDSSITUASJON
        }?.fakta?.firstOrNull()

        private fun hentFullstendigManglendeInnbetaling(avklartefakta: Set<AvklartefaktaDto>): Boolean = avklartefakta.firstOrNull {
            it.avklartefaktaType == Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING
        }?.fakta?.single().toBoolean()

        private fun hentOppholdType(avklartefakta: Set<AvklartefaktaDto>): String? = avklartefakta.firstOrNull {
            it.avklartefaktaType == Avklartefaktatyper.IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD
        }?.fakta?.firstOrNull()

        private fun hentUkjentSluttdato(avklartefakta: Set<AvklartefaktaDto>): Boolean = avklartefakta.firstOrNull {
            it.avklartefaktaType == Avklartefaktatyper.UKJENT_SLUTTDATO
        }?.fakta?.single().toBoolean()
    }
}
