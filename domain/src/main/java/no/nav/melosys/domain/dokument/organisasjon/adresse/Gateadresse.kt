package no.nav.melosys.domain.dokument.organisasjon.adresse;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * En geografisk adresse som angir geografisk plassering i veiadresse form. Vil brukes om adresser i Norge.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Gateadresse")
public class Gateadresse extends StrukturertAdresse {

    @XmlElement(required = true)
    private String poststed;

    private String bolignummer;

    private String kommunenummer;

    private Integer gatenummer;

    @XmlElement(required = true)
    private String gatenavn;

    private Integer husnummer;

    private String husbokstav;

    public String getPoststed() {
        return poststed;
    }

    public void setPoststed(String poststed) {
        this.poststed = poststed;
    }

    public String getBolignummer() {
        return bolignummer;
    }

    public void setBolignummer(String bolignummer) {
        this.bolignummer = bolignummer;
    }

    public String getKommunenummer() {
        return kommunenummer;
    }

    public void setKommunenummer(String kommunenummer) {
        this.kommunenummer = kommunenummer;
    }

    public Integer getGatenummer() {
        return gatenummer;
    }

    public void setGatenummer(Integer value) {
        this.gatenummer = value;
    }

    public String getGatenavn() {
        return gatenavn;
    }

    public void setGatenavn(String value) {
        this.gatenavn = value;
    }

    public Integer getHusnummer() {
        return husnummer;
    }

    public void setHusnummer(Integer value) {
        this.husnummer = value;
    }

    public String getHusbokstav() {
        return husbokstav;
    }

    public void setHusbokstav(String value) {
        this.husbokstav = value;
    }

}