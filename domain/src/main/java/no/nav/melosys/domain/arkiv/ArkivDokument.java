package no.nav.melosys.domain.arkiv;

import java.util.ArrayList;
import java.util.List;

public class ArkivDokument {
    private String dokumentId;
    private List<ArkivDokumentVedlegg> interneVedlegg; // Til sammensatte dokumenter der vedlegg er scannet inn i ett dokument.
    private String tittel;
    private String navSkjemaID;

    public ArkivDokument() {
        this.interneVedlegg = new ArrayList<>();
    }

    public String getDokumentId() {
        return dokumentId;
    }

    public void setDokumentId(String dokumentId) {
        this.dokumentId = dokumentId;
    }

    public List<ArkivDokumentVedlegg> getInterneVedlegg() {
        return interneVedlegg;
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
