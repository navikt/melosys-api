package no.nav.melosys.domain.dokument.person.adresse;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MidlertidigPostadresseUtland")
public class MidlertidigPostadresseUtland extends MidlertidigPostadresse {

    public String adresselinje1;

    public String adresselinje2;

    public String adresselinje3;

    public String adresselinje4;

}
