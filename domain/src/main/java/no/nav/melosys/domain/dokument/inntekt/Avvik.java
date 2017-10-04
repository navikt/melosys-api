package no.nav.melosys.domain.dokument.inntekt;


import java.time.YearMonth;

import javax.xml.bind.annotation.XmlElement;

public class Avvik {

    @XmlElement(required = true)
    private String ident;

    @XmlElement(required = true)
    private String opplysningspliktigID;

    private String virksomhetID;

    @XmlElement(required = true)
    private YearMonth avvikPeriode;

    @XmlElement(required = true)
    private String tekst;
}
