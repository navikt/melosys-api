package no.nav.melosys.domain.dokument.arbeidsforhold;

public enum Aktoertype {

    ORGANISASJON("Organisasjonsnummer i Brønnøysundregistrene."),

    PERSON("Fødselsnr, SSN..."),

    HISTORISK_ARBEIDSGIVER_MED_ARBEIDSGIVERNUMMER("Arbeidsgiver som har opphørt å eksistere før enhetsregister ble etablert.");

    private String dokumentasjon;

    Aktoertype(String dokumentasjon) {
        this.dokumentasjon = dokumentasjon;
    }
}
