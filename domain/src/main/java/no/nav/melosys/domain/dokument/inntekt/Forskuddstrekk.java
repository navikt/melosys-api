package no.nav.melosys.domain.dokument.inntekt;

import java.time.LocalDateTime;
import java.time.YearMonth;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;

// FIXME Ikke implementert i xslt
@XmlAccessorType(XmlAccessType.FIELD)
public class Forskuddstrekk {

    protected int beloep;

    protected String beskrivelse;

    protected YearMonth forskuddstrekkPeriode;

    @XmlSchemaType(name = "dateTime")
    protected LocalDateTime leveringstidspunkt;

    protected String opplysningspliktigID;

    protected String innsenderID;

    protected String utbetalerID;

    protected String forskuddstrekkGjelderID;

    public int getBeloep() {
        return beloep;
    }

    public void setBeloep(int beloep) {
        this.beloep = beloep;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public void setBeskrivelse(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public YearMonth getForskuddstrekkPeriode() {
        return forskuddstrekkPeriode;
    }

    public void setForskuddstrekkPeriode(YearMonth forskuddstrekkPeriode) {
        this.forskuddstrekkPeriode = forskuddstrekkPeriode;
    }

    public LocalDateTime getLeveringstidspunkt() {
        return leveringstidspunkt;
    }

    public void setLeveringstidspunkt(LocalDateTime leveringstidspunkt) {
        this.leveringstidspunkt = leveringstidspunkt;
    }

    public String getOpplysningspliktigID() {
        return opplysningspliktigID;
    }

    public void setOpplysningspliktigID(String opplysningspliktigID) {
        this.opplysningspliktigID = opplysningspliktigID;
    }

    public String getInnsenderID() {
        return innsenderID;
    }

    public void setInnsenderID(String innsenderID) {
        this.innsenderID = innsenderID;
    }

    public String getUtbetalerID() {
        return utbetalerID;
    }

    public void setUtbetalerID(String utbetalerID) {
        this.utbetalerID = utbetalerID;
    }

    public String getForskuddstrekkGjelderID() {
        return forskuddstrekkGjelderID;
    }

    public void setForskuddstrekkGjelderID(String forskuddstrekkGjelderID) {
        this.forskuddstrekkGjelderID = forskuddstrekkGjelderID;
    }
}
