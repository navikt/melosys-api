package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.Kontaktopplysning

@JvmRecord
data class KontaktInfoDto(@JvmField val kontaktnavn: String?, @JvmField val kontaktorgnr: String?, @JvmField val kontakttelefon: String?) {
    companion object {
        @JvmStatic
        fun av(kontaktopplysning: Kontaktopplysning): KontaktInfoDto {
            return KontaktInfoDto(
                kontaktopplysning.kontaktNavn,
                kontaktopplysning.kontaktOrgnr,
                kontaktopplysning.kontaktTelefon
            )
        }
    }
}
