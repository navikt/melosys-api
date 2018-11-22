package no.nav.melosys.service.journalforing.dto;

public class DokumentDto {
    public final String dokumentID;
    public final String tittel;

    public DokumentDto(String tittel) {
        this.dokumentID = null;
        this.tittel = tittel;
    }
}