package no.nav.melosys.domain.dokument.arbeidsforhold;

public enum Aktoertype {
    Organisasjon("Organisasjonsnummer i Brønnøysundregistrene."),
    Person("Fødselsnr, SSN..."),
    HistoriskArbeidsgiverMedArbeidsgivernummer("Arbeidsgiver som har opphørt å eksistere før enhetsregister ble etablert, vil ikke ha blitt konvertert til ny organisasjonsstruktur med orgnummer. Arbeidsforhold knyttet til disse historiske arbeidsgiverne vil bruke det nedestående formatet.");

    private String dokumentasjon;

    Aktoertype(String dokumentasjon) {
        this.dokumentasjon = dokumentasjon;
    }

    public String getDokumentasjon() {
        return dokumentasjon;
    }
}
