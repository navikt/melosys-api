package no.nav.melosys.domain.person.familie

data class IkkeOmfattetFamilie(
    val uuid: String,
    val begrunnelse: String?,
    val begrunnelseFritekst: String?
) {
    var sammensattNavn: String? = null
    var ident: String? = null
}
