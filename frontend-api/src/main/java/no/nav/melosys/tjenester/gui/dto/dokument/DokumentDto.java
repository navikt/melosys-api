package no.nav.melosys.tjenester.gui.dto.dokument;

import java.util.ArrayList;
import java.util.List;

public class DokumentDto {
    public final String dokumentID;
    public final String tittel;
    public final List<String> logsikeVedlegg;

    public DokumentDto(String tittel) {
        this.dokumentID = null;
        this.tittel = tittel;
        this.logsikeVedlegg = new ArrayList<>();
    }

    public DokumentDto(String dokumentID, String tittel, List<String> logiskeVedlegg) {
        this.dokumentID = dokumentID;
        this.tittel = tittel;
        this.logsikeVedlegg = logiskeVedlegg;
    }
}
