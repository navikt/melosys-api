package no.nav.melosys.tjenester.gui.dto.oppsummertefakta

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto

class ArbeidslandDto {
    @JvmField
    var arbeidsland: List<String>? = null

    companion object {
        fun av(avklartefaktas: Set<AvklartefaktaDto>): ArbeidslandDto {
            val land: MutableList<String> = ArrayList()
            avklartefaktas.stream()
                .filter { avklartefakta: AvklartefaktaDto -> Avklartefaktatyper.ARBEIDSLAND.kode == avklartefakta.referanse && Avklartefaktatyper.ARBEIDSLAND == avklartefakta.avklartefaktaType }
                .forEach { avklartefakta: AvklartefaktaDto -> land.add(avklartefakta.subjektID) }

            val arbeidslandDto = ArbeidslandDto()
            arbeidslandDto.arbeidsland = land
            return arbeidslandDto
        }
    }
}
