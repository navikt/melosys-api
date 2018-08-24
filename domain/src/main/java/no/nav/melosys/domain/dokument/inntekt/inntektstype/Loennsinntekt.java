package no.nav.melosys.domain.dokument.inntekt.inntektstype;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Loennsinntekt")
public class Loennsinntekt
        extends Inntekt
{

    @XmlElement(required = true)
    protected String beskrivelse; //"http://nav.no/kodeverk/Kodeverk/Loennsbeskrivelse"s

    @JsonIgnore
    protected Integer antall;

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public void setBeskrivelse(String value) {
        this.beskrivelse = value;
    }

    public Integer getAntall() {
        return antall;
    }

    public void setAntall(Integer value) {
        this.antall = value;
    }

}
