package no.nav.melosys.domain.dokument.inntekt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;

// FIXME Ikke implementert i xslt
@XmlAccessorType(XmlAccessType.FIELD)
public class Fradrag {

    @XmlElement(required = true)
    protected BigDecimal beloep;

    @XmlElement(required = true)
    protected String beskrivelse;

    protected YearMonth fradragsperiode;

    @XmlSchemaType(name = "dateTime")
    protected LocalDateTime leveringstidspunkt;

    protected String inntektspliktigID;

    protected String utbetalerID;

    protected String innsenderID;

    protected String fradragGjelderID;

    public BigDecimal getBeloep() {
        return beloep;
    }

    public void setBeloep(BigDecimal beloep) {
        this.beloep = beloep;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public void setBeskrivelse(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public YearMonth getFradragsperiode() {
        return fradragsperiode;
    }

    public void setFradragsperiode(YearMonth fradragsperiode) {
        this.fradragsperiode = fradragsperiode;
    }

    public LocalDateTime getLeveringstidspunkt() {
        return leveringstidspunkt;
    }

    public void setLeveringstidspunkt(LocalDateTime leveringstidspunkt) {
        this.leveringstidspunkt = leveringstidspunkt;
    }

    public String getInntektspliktigID() {
        return inntektspliktigID;
    }

    public void setInntektspliktigID(String inntektspliktigID) {
        this.inntektspliktigID = inntektspliktigID;
    }

    public String getUtbetalerID() {
        return utbetalerID;
    }

    public void setUtbetalerID(String utbetalerID) {
        this.utbetalerID = utbetalerID;
    }

    public String getInnsenderID() {
        return innsenderID;
    }

    public void setInnsenderID(String innsenderID) {
        this.innsenderID = innsenderID;
    }

    public String getFradragGjelderID() {
        return fradragGjelderID;
    }

    public void setFradragGjelderID(String fradragGjelderID) {
        this.fradragGjelderID = fradragGjelderID;
    }
}
