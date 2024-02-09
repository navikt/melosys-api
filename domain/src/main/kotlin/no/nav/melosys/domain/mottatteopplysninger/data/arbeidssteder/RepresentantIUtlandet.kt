package no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder

import no.nav.melosys.domain.kodeverk.Landkoder


class RepresentantIUtlandet {
    var representantNavn: String? = null
    var adresselinjer: List<String> = ArrayList()
    var representantLand: String? = null

    companion object {
        fun av(representantNavn: String?, adresselinjer: List<String>, representantLand: Landkoder): RepresentantIUtlandet {
            val representantIUtlandet = RepresentantIUtlandet()
            representantIUtlandet.representantNavn = representantNavn
            representantIUtlandet.adresselinjer = adresselinjer
            representantIUtlandet.representantLand = representantLand.kode
            return representantIUtlandet
        }
    }
}
