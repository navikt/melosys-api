package no.nav.melosys.domain.dokument.utbetaling;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class Ytelse {

    public String type;

    public Aktoer rettighetshaver;

    public Periode periode;
}
