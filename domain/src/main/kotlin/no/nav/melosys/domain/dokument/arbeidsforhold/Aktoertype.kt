package no.nav.melosys.domain.dokument.arbeidsforhold

enum class Aktoertype(private val dokumentasjon: String) {
    ORGANISASJON("Organisasjonsnummer i Brønnøysundregistrene."),
    PERSON("Fødselsnr, SSN..."),
    HISTORISK_ARBEIDSGIVER_MED_ARBEIDSGIVERNUMMER("Arbeidsgiver som har opphørt å eksistere før enhetsregister ble etablert.")
}
