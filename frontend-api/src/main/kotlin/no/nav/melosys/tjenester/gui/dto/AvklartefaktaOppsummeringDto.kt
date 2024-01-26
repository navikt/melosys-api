package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.tjenester.gui.dto.oppsummertefakta.ArbeidslandDto
import no.nav.melosys.tjenester.gui.dto.oppsummertefakta.VirksomheterDto

class AvklartefaktaOppsummeringDto {
    @JvmField
    var virksomheter: VirksomheterDto? = null
    @JvmField
    var arbeidsland: ArbeidslandDto? = null
    @JvmField
    var fullstendigManglendeInnbetaling: Boolean? = null

    companion object {
        @JvmStatic
        fun av(avklartefakta: Set<AvklartefaktaDto>): AvklartefaktaOppsummeringDto {
            val avklartefaktaOppsummeringDto = AvklartefaktaOppsummeringDto()
            avklartefaktaOppsummeringDto.virksomheter = VirksomheterDto.av(avklartefakta)
            avklartefaktaOppsummeringDto.arbeidsland = ArbeidslandDto.av(avklartefakta)
            avklartefaktaOppsummeringDto.fullstendigManglendeInnbetaling =
                hentFullstendigManglendeInnbetaling(avklartefakta)
            return avklartefaktaOppsummeringDto
        }

        @JvmStatic
        fun hentFullstendigManglendeInnbetaling(avklartefaktas: Set<AvklartefaktaDto>): Boolean? {
            val avklartefaktaDto = avklartefaktas.stream()
                .filter { avklartefakta: AvklartefaktaDto -> Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING.kode == avklartefakta.referanse && Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING == avklartefakta.avklartefaktaType }
                .limit(1)
                .findFirst()
            var fullstendigManglendeInnbetaling: Boolean? = null
            if (avklartefaktaDto.isPresent) {
                fullstendigManglendeInnbetaling = avklartefaktaDto.get().fakta[0].toBoolean()
            }
            return fullstendigManglendeInnbetaling
        }
    }
}
