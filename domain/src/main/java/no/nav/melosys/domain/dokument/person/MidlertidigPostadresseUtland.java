package no.nav.melosys.domain.dokument.person;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MidlertidigPostadresseUtland")
@XmlSeeAlso(UstrukturertAdresse.class)
public class MidlertidigPostadresseUtland extends MidlertidigPostadresse {
    public String test;

    public UstrukturertAdresse  ustrukturertAdresse;
}
