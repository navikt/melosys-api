package no.nav.melosys.domain.arkiv;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ArkivDokument {
    private String dokumentId;
    private final List<LogiskVedlegg> logiskeVedlegg; // Til sammensatte dokumenter der vedlegg er scannet inn i ett dokument.
    private final List<DokumentVariant> dokumentVarianter;
    private String tittel;
    private String navSkjemaID;

    public ArkivDokument() {
        this.logiskeVedlegg = new ArrayList<>();
        this.dokumentVarianter = new ArrayList<>();
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

    public List<String> hentLogiskeVedleggTitler() {
        return logiskeVedlegg.stream().map(LogiskVedlegg::tittel).collect(Collectors.toList());
    }

    public void setLogiskeVedleggTitler(List<LogiskVedlegg> logiskeVedlegg) {
        this.logiskeVedlegg.addAll(logiskeVedlegg);
    }

    public List<DokumentVariant> getDokumentVarianter() {
        return dokumentVarianter;
    }

    public void setDokumentVarianter(List<DokumentVariant> dokumentVarianter) {
        this.dokumentVarianter.addAll(dokumentVarianter);
    }


    public boolean harTilgangTilArkivVariant() {
        return dokumentVarianter.stream()
            .filter(DokumentVariant::erVariantArkiv)
            .allMatch(DokumentVariant::getSaksbehandlerHarTilgang);
    }
}
