package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland

class SoeknadslandDto(@JvmField var landkoder: List<String>, @JvmField var erUkjenteEllerAlleEosLand: Boolean) {
    companion object {
        @JvmStatic
        fun av(søknadsland: Soeknadsland?): SoeknadslandDto {
            if (søknadsland == null) {
                return SoeknadslandDto(emptyList(), false)
            }
            return SoeknadslandDto(søknadsland.landkoder, søknadsland.erUkjenteEllerAlleEosLand)
        }

        @JvmStatic
        fun av(landkode: Landkoder?): SoeknadslandDto {
            val landkoder = if (landkode != null) listOf(landkode.kode) else emptyList()
            return SoeknadslandDto(landkoder, false)
        }
    }
}
