package no.nav.melosys.domain.arkiv;

import java.util.ArrayList;
import java.util.List;

public class ArkivDokument {
    private String dokumentId;
    private List<LogiskeVedlegg> logiskeVedlegg; // Til sammensatte dokumenter der vedlegg er scannet inn i ett dokument.
    private String tittel;
    private String navSkjemaID;

    public ArkivDokument() {
        this.logiskeVedlegg = new ArrayList<>();
    }

    public String getDokumentId() {
        return dokumentId;
    }

    public void setDokumentId(String dokumentId) {
        this.dokumentId = dokumentId;
    }

    public List<LogiskeVedlegg> getLogiskeVedlegg() {
        return logiskeVedlegg;
    }

    public String getTittel() {
        return tittel;
    }

    public void setTittel(String tittel) {
        this.tittel = tittel;
    }

    public String getNavSkjemaID() {
        return navSkjemaID;
    }

    public void setNavSkjemaID(String navSkjemaID) {
        this.navSkjemaID = navSkjemaID;
    }
}
