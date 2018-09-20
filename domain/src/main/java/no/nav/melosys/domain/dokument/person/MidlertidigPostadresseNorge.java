package no.nav.melosys.domain.dokument.person;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MidlertidigPostadresseNorge")
public class MidlertidigPostadresseNorge extends MidlertidigPostadresse {

    public String tilleggsadresse;

    public String tilleggsadresseType;

    public String poststed;

    public String bolignummer;

    public String kommunenummer;

    public Gateadresse gateadresse;
}
