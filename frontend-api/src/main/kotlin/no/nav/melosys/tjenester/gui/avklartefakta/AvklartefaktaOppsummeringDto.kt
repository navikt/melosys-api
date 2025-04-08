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
    val ukjentSluttdatoMedlemskapsperiode: Boolean?,
    val betalingsvalg: String?
) {
    constructor(avklartefakta: Set<AvklartefaktaDto>) : this(
        virksomheter = VirksomheterDto.av(avklartefakta),
        arbeidsland = ArbeidslandDto.av(avklartefakta),
        fullstendigManglendeInnbetaling = hentFullstendigManglendeInnbetaling(avklartefakta),
        ikkeYrkesaktivFamilieRelasjonstype = hentFamileRelasjonType(avklartefakta),
        ikkeYrkesaktivOppholdstype = hentOppholdType(avklartefakta),
        arbeidssituasjonType = hentArbeidssituasjonType(avklartefakta),
        ukjentSluttdatoMedlemskapsperiode = hentUkjentSluttdatoMedlemskapsperiode(avklartefakta),
        betalingsvalg = hentBetalingsvalg(avklartefakta)
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

        private fun hentUkjentSluttdatoMedlemskapsperiode(avklartefakta: Set<AvklartefaktaDto>): Boolean = avklartefakta.firstOrNull {
            it.avklartefaktaType == Avklartefaktatyper.UKJENT_SLUTTDATO_MEDLEMSKAPSPERIODE
        }?.fakta?.single().toBoolean()

        private fun hentBetalingsvalg(avklartefakta: Set<AvklartefaktaDto>): String? = avklartefakta.firstOrNull {
            it.avklartefaktaType == Avklartefaktatyper.BETALINGSVALG
        }?.fakta?.firstOrNull()
    }
}
