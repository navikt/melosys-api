package no.nav.melosys.service.journalforing.dto;

public class DokumentDto {
    public String dokumentID;
    public String tittel;

    public DokumentDto() {
    }

    public DokumentDto(String tittel) {
        this.dokumentID = null;
        this.tittel = tittel;
    }
}