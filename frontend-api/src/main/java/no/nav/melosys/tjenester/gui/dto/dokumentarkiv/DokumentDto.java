package no.nav.melosys.tjenester.gui.dto.dokumentarkiv;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.DokumentVariant;

public class DokumentDto {
    public final String dokumentID;
    public final String tittel;
    public final List<String> logiskeVedlegg;
    public final DokumentVariant.Filtype filtype;

    public DokumentDto(String tittel) {
        this.dokumentID = null;
        this.tittel = tittel;
        this.logiskeVedlegg = new ArrayList<>();
        this.filtype = null;
    }

    public DokumentDto(String dokumentID, String tittel, List<String> logiskeVedlegg, DokumentVariant.Filtype filtype) {
        this.dokumentID = dokumentID;
        this.tittel = tittel;
        this.logiskeVedlegg = logiskeVedlegg;
        this.filtype = filtype;
    }

    public static DokumentDto av(ArkivDokument dokument) {
        return new DokumentDto(
            dokument.getDokumentId(),
            dokument.getTittel(),
            dokument.hentLogiskeVedleggTitler(),
            dokument.arkivVariantFiltype()
        );
    }
}
