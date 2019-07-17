package no.nav.melosys.service.journalforing.dto;

import org.apache.commons.lang3.StringUtils;

public class DokumentDto {
    private String dokumentID;
    private String tittel;

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

    public boolean erLogiskVedlegg(String hovedDokumentID) {
        return StringUtils.isEmpty(dokumentID) || dokumentID.equals(hovedDokumentID);
    }

    public boolean erFysiskVedlegg(String hovedDokumentID) {
        return !erLogiskVedlegg(hovedDokumentID);
    }
}