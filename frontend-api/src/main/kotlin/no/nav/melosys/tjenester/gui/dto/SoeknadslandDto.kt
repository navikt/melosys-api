package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland

data class SoeknadslandDto(
    var landkoder: List<String> = emptyList(),
    var flereLandUkjentHvilke: Boolean = false
) {
    companion object {
        @JvmStatic
        fun av(søknadsland: Soeknadsland?): SoeknadslandDto {
            return if (søknadsland == null) {
                SoeknadslandDto(emptyList(), false)
            } else {
                SoeknadslandDto(søknadsland.landkoder, søknadsland.isFlereLandUkjentHvilke)
            }
        }

        @JvmStatic
        fun av(landkode: Landkoder?): SoeknadslandDto {
            val landkoder = landkode?.kode?.let { listOf(it) } ?: emptyList()
            return SoeknadslandDto(landkoder, false)
        }
    }
}
