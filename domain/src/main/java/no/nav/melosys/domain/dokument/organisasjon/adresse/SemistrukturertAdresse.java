package no.nav.melosys.domain.dokument.organisasjon.adresse;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SemistrukturertAdresse")
public class SemistrukturertAdresse extends GeografiskAdresse {

    private String adresselinje1;

    private String adresselinje2;

    private String adresselinje3;

    private String postnr;

    private String kommunenr;

    @JsonIgnore
    private String poststedUtland;

    public String getAdresselinje1() {
        return adresselinje1;
    }

    public void setAdresselinje1(String adresselinje1) {
        this.adresselinje1 = adresselinje1;
    }

    public String getAdresselinje2() {
        return adresselinje2;
    }

    public void setAdresselinje2(String adresselinje2) {
        this.adresselinje2 = adresselinje2;
    }

    public String getAdresselinje3() {
        return adresselinje3;
    }

    public void setAdresselinje3(String adresselinje3) {
        this.adresselinje3 = adresselinje3;
    }

    public String getPostnr() {
        return postnr;
    }

    public void setPostnr(String postnr) {
        this.postnr = postnr;
    }

    public String getKommunenr() {
        return kommunenr;
    }

    public void setKommunenr(String kommunenr) {
        this.kommunenr = kommunenr;
    }

    public String getPoststedUtland() {
        return poststedUtland;
    }

    public void setPoststedUtland(String poststedUtland) {
        this.poststedUtland = poststedUtland;
    }
}