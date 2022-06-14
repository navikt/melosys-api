package no.nav.melosys.service.dokument;

import java.util.Map;

public record DokumentproduksjonsInfo(String dokgenMalnavn, String dokumentKategoriKode, String journalføringsTittel, Map<VedleggTyper, String> vedleggsTitler) {

    public DokumentproduksjonsInfo(String dokgenMalnavn, String dokumentKategoriKode, String journalføringsTittel) {
        this(dokgenMalnavn, dokumentKategoriKode, journalføringsTittel, null);
    }

    public DokumentproduksjonsInfo(String dokgenMalnavn, String dokumentKategoriKode) {
        this(dokgenMalnavn, dokumentKategoriKode, null, null);
    }
}
