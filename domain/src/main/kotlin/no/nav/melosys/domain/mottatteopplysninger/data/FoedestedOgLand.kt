package no.nav.melosys.domain.mottatteopplysninger.data


class FoedestedOgLand {
    var foedested: String? = null
        private set
    var foedeland: String? = null
        private set

    constructor()
    constructor(foedested: String?, foedeland: String?) {
        this.foedested = foedested
        this.foedeland = foedeland
    }
}

