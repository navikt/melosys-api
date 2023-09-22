package no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Telefonnummer")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Telefonnummer extends ElektroniskAdresse {

    private String identifikator;
    private String type; // http://nav.no/kodeverk/Kodeverk/Telefontyper

    public String getIdentifikator() {
        return identifikator;
    }

    public void setIdentifikator(String identifikator) {
        this.identifikator = identifikator;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
