package no.nav.melosys.domain.dokument.inntekt.inntektstype;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import no.nav.melosys.domain.dokument.inntekt.Inntekt;

@XmlType(name = "Naeringsinntekt")
public class Naeringsinntekt extends Inntekt {

    @XmlElement(required = true)
    protected String beskrivelse; // http://nav.no/kodeverk/Kodeverk/Naeringsinntektsbeskrivelse

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public void setBeskrivelse(String value) {
        this.beskrivelse = value;
    }
}
