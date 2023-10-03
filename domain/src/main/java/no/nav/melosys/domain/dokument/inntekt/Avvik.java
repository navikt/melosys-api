package no.nav.melosys.domain.dokument.inntekt;


import java.time.YearMonth;

import org.jetbrains.annotations.NotNull;

public class Avvik {

    @NotNull
    private String ident;

    @NotNull
    private String opplysningspliktigID;

    private String virksomhetID;

    @NotNull
    private YearMonth avvikPeriode;

    @NotNull
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
