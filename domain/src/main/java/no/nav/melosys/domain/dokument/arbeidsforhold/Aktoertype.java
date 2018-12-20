package no.nav.melosys.domain.dokument.arbeidsforhold;

import javax.xml.bind.annotation.XmlEnumValue;

public enum Aktoertype {

    @XmlEnumValue("Organisasjon")
    ORGANISASJON("Organisasjonsnummer i Brønnøysundregistrene."),
    @XmlEnumValue("Person")
    PERSON("Fødselsnr, SSN..."),
    @XmlEnumValue("HistoriskArbeidsgiverMedArbeidsgivernummer")
    HISTORISK_ARBEIDSGIVER_MED_ARBEIDSGIVERNUMMER("Arbeidsgiver som har opphørt å eksistere før enhetsregister ble etablert.");

    private String dokumentasjon;

    Aktoertype(String dokumentasjon) {
        this.dokumentasjon = dokumentasjon;
    }
}
