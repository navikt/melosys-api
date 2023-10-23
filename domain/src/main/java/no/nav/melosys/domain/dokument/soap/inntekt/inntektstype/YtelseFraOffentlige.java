package no.nav.melosys.domain.dokument.soap.inntekt.inntektstype;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import no.nav.melosys.domain.dokument.soap.inntekt.Inntekt;

@XmlType(name = "YtelseFraOffentlige")
public class YtelseFraOffentlige extends Inntekt {

    @XmlElement(required = true)
    protected String beskrivelse; // http://nav.no/kodeverk/Kodeverk/YtelseFraOffentligeBeskrivelse

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }

    public void setBeskrivelse(String value) {
        this.beskrivelse = value;
    }
}
