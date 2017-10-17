package no.nav.melosys.domain.dokument.inntekt.inntektstype;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import no.nav.melosys.domain.dokument.inntekt.Inntekt;

@XmlType(name = "PensjonEllerTrygd")
public class PensjonEllerTrygd extends Inntekt {

    @XmlElement(required = true)
    protected String beskrivelse; // http://nav.no/kodeverk/Kodeverk/PensjonEllerTrygdebeskrivelse

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public void setBeskrivelse(String value) {
        this.beskrivelse = value;
    }
}
