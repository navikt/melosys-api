package no.nav.melosys.domain.person.familie

class OmfattetFamilie(
    val uuid: String
) {
    var sammensattNavn: String? = null
    var ident: String? = null

    override fun toString() = "OmfattetFamilie{" +
        "uuid='$uuid', " +
        "sammensattNavn='$sammensattNavn', " +
        "ident='$ident'" +
        "}"
}