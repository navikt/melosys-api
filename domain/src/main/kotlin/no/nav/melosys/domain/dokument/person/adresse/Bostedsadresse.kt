package no.nav.melosys.domain.dokument.person.adresse

import com.fasterxml.jackson.annotation.JsonView
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.dokument.DokumentView
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.util.IsoLandkodeKonverterer


data class Bostedsadresse(
    var gateadresse: Gateadresse = Gateadresse(),
    var land: Land = Land(),
    @JsonView(DokumentView.Database::class)
    var tilleggsadresse: String? = null,
    @JsonView(DokumentView.Database::class)
    var tilleggsadresseType: String? = null,
    var postnr: String? = null,
    var poststed: String? = null
) {
    fun erTom(): Boolean = (gateadresse.erTom() && postnr.isNullOrEmpty() && poststed.isNullOrEmpty() && land.kode.isNullOrEmpty())

    fun tilStrukturertAdresse(): StrukturertAdresse {
        val adresse = StrukturertAdresse()
        gateadresse.let {
            adresse.gatenavn = it.gatenavn
            adresse.husnummerEtasjeLeilighet = it.husnummer.toString() + it.husbokstav.orEmpty()
        }
        adresse.postnummer = postnr
        adresse.poststed = poststed
        if (!land.kode.isNullOrEmpty()) {
            adresse.landkode = IsoLandkodeKonverterer.tilIso2(land.kode)
        }
        return adresse
    }
}


