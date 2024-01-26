package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import java.util.stream.Collectors

@JvmRecord
data class MedfolgendeFamilieDto(
    val uuid: String?,
    val omfattet: Boolean?,
    val begrunnelseKode: String?,
    val begrunnelseFritekst: String?
) {

    fun erIkkeOmfattet(): Boolean {
        return omfattet == false
    }

    fun erOmfattet(): Boolean {
        return omfattet == true
    }



    companion object {
        @JvmStatic
        fun av(avklartefaktas: Set<AvklartefaktaDto>): Set<MedfolgendeFamilieDto> {
            return avklartefaktas.stream()
                .filter { avklartfakta: AvklartefaktaDto -> erMedfolgendeFamilieFakta(avklartfakta) }
                .map { avklartefakta: AvklartefaktaDto ->
                    MedfolgendeFamilieDto(
                        avklartefakta.subjektID,
                        tilBoolean(avklartefakta.fakta),
                        avklartefakta.begrunnelseKoder.stream().findFirst().orElse(null),
                        avklartefakta.begrunnelseFritekst
                    )
                }
                .collect(Collectors.toSet())
        }
        private fun erMedfolgendeFamilieFakta(avklartfakta: AvklartefaktaDto): Boolean {
            return Avklartefaktatyper.VURDERING_LOVVALG_BARN.kode == avklartfakta.referanse || Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.kode == avklartfakta.referanse
        }

        private fun tilBoolean(fakta: List<String>): Boolean {
            return Avklartefakta.VALGT_FAKTA == fakta[0]
        }
    }
}
