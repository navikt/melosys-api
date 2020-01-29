package no.nav.melosys.domain.arkiv;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ArkivDokument {
    private String dokumentId;
    private List<LogiskVedlegg> logiskeVedlegg; // Til sammensatte dokumenter der vedlegg er scannet inn i ett dokument.
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

    public List<LogiskVedlegg> getLogiskeVedlegg() {
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

    public List<String> mapLogiskeVedlegg() {
        return logiskeVedlegg.stream().map(LogiskVedlegg::getTittel).collect(Collectors.toList());
    }
}
