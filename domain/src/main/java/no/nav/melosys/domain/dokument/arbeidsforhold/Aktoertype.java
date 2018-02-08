package no.nav.melosys.domain.dokument.arbeidsforhold;

public enum Aktoertype {

    // FIXME: Skal være allcaps
    Organisasjon("Organisasjonsnummer i Brønnøysundregistrene."),
    Person("Fødselsnr, SSN..."),
    HistoriskArbeidsgiverMedArbeidsgivernummer("Arbeidsgiver som har opphørt å eksistere før enhetsregister ble etablert.");

    private String dokumentasjon;

    Aktoertype(String dokumentasjon) {
        this.dokumentasjon = dokumentasjon;
    }

    public String getDokumentasjon() {
        return dokumentasjon;
    }
}
