package no.nav.melosys.domain.dokument.arbeidsforhold


class Yrke {
    var kode: String? = null
    var term: String? = null

    // Brukes av JAXB
    constructor()
    constructor(yrkeKode: String?) {
        kode = yrkeKode
    }
}

