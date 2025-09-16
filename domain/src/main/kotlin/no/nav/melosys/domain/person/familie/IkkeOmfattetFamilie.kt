package no.nav.melosys.domain.person.familie

class IkkeOmfattetFamilie(
    val uuid: String,
    val begrunnelse: String?,
    val begrunnelseFritekst: String?
) {
    var sammensattNavn: String? = null
    var ident: String? = null

    override fun toString(): String =
        "IkkeOmfattetFamilie{" +
            "uuid='$uuid', " +
            "begrunnelse='$begrunnelse', " +
            "begrunnelseFritekst='$begrunnelseFritekst', " +
            "sammensattNavn='$sammensattNavn', " +
            "ident='$ident'" +
            "}"
}