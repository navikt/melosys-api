package no.nav.melosys.service.dokument;

import java.util.Map;

public record DokumentproduksjonsInfo(String dokgenMalnavn, String dokumentKategoriKode, String journalføringsTittel, String alternativTittel,
                                      Map<VedleggTyper, String> vedleggsTitler) {


    public DokumentproduksjonsInfo(String dokgenMalnavn, String dokumentKategoriKode, String journalføringsTittel, String alternativTittel) {
        this(dokgenMalnavn, dokumentKategoriKode, journalføringsTittel, alternativTittel, null);
    }

    public DokumentproduksjonsInfo(String dokgenMalnavn, String dokumentKategoriKode, String journalføringsTittel) {
        this(dokgenMalnavn, dokumentKategoriKode, journalføringsTittel, null, null);
    }

    public String getAttestTittel() {
        return vedleggsTitler.get(VedleggTyper.ATTEST);
    }
}
