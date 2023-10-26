package no.nav.melosys.service.journalforing.dto;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.saksflytapi.journalfoering.DokumentRequest;

public class DokumentDto {
    private String dokumentID;
    private String tittel;
    private List<String> logiskeVedlegg = new ArrayList<>();

    private DokumentDto() {
        // Jackson
    }

    public DokumentDto(String dokumentID, String tittel) {
        this.dokumentID = dokumentID;
        this.tittel = tittel;
    }

    public String getDokumentID() {
        return dokumentID;
    }

    public void setDokumentID(String dokumentID) {
        this.dokumentID = dokumentID;
    }

    public String getTittel() {
        return tittel;
    }

    public void setTittel(String tittel) {
        this.tittel = tittel;
    }

    public List<String> getLogiskeVedlegg() {
        return logiskeVedlegg;
    }

    public void setLogiskeVedlegg(List<String> logiskeVedlegg) {
        this.logiskeVedlegg = logiskeVedlegg;
    }

    public DokumentRequest tilDokumentRequest() {
        return new DokumentRequest(dokumentID, tittel, logiskeVedlegg);
    }
}
