package no.nav.melosys.domain.dokument.organisasjon.adresse;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

//FIXME (Francois) mangler EREG testdata for å teste StrukturertAdresse
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StrukturertAdresse")
public abstract class StrukturertAdresse extends GeografiskAdresse {

    protected String tilleggsadresse;

    protected String tilleggsadresseType;

    public String getTilleggsadresse() {
        return tilleggsadresse;
    }

    public void setTilleggsadresse(String value) {
        this.tilleggsadresse = value;
    }

    public String getTilleggsadresseType() {
        return tilleggsadresseType;
    }

    public void setTilleggsadresseType(String value) {
        this.tilleggsadresseType = value;
    }

}
