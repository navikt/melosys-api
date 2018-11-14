package no.nav.melosys.tjenester.gui.dto.dokument;

public class DokumentDto {
    public final String dokumentID;
    public final String tittel;

    public DokumentDto(String tittel) {
        this.dokumentID = null;
        this.tittel = tittel;
    }

    public DokumentDto(String dokumentID, String tittel) {
        this.dokumentID = dokumentID;
        this.tittel = tittel;
    }
}
