package no.nav.melosys.domain.dokument.inntekt;


import java.time.YearMonth;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
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

    public String getIdent() {
        return ident;
    }

    public void setIdent(String ident) {
        this.ident = ident;
    }

    public String getOpplysningspliktigID() {
        return opplysningspliktigID;
    }

    public void setOpplysningspliktigID(String opplysningspliktigID) {
        this.opplysningspliktigID = opplysningspliktigID;
    }

    public String getVirksomhetID() {
        return virksomhetID;
    }

    public void setVirksomhetID(String virksomhetID) {
        this.virksomhetID = virksomhetID;
    }

    public YearMonth getAvvikPeriode() {
        return avvikPeriode;
    }

    public void setAvvikPeriode(YearMonth avvikPeriode) {
        this.avvikPeriode = avvikPeriode;
    }

    public String getTekst() {
        return tekst;
    }

    public void setTekst(String tekst) {
        this.tekst = tekst;
    }
}
