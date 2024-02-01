package no.nav.melosys.tjenester.gui.dto.oppsummertefakta

import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto

class VirksomheterDto {
    @JvmField
    var virksomhetIDer: List<String>? = null

    companion object {
        fun av(avklartefaktas: Set<AvklartefaktaDto>): VirksomheterDto {
            val virksomheter: MutableList<String> = ArrayList()
            avklartefaktas.stream()
                .filter { avklartefakta: AvklartefaktaDto -> Avklartefaktatyper.VIRKSOMHET.kode == avklartefakta.referanse && Avklartefaktatyper.VIRKSOMHET == avklartefakta.avklartefaktaType }
                .forEach { avklartefakta: AvklartefaktaDto -> virksomheter.add(avklartefakta.subjektID) }

            val virksomheterDto = VirksomheterDto()
            virksomheterDto.virksomhetIDer = virksomheter
            return virksomheterDto
        }
    }
}
