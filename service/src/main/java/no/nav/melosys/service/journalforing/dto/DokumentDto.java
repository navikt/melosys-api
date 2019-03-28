package no.nav.melosys.service.journalforing.dto;

public class DokumentDto {
    private String dokumentID;
    private String tittel;

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
}