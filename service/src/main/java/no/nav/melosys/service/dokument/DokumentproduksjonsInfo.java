package no.nav.melosys.service.dokument;

import java.util.Map;

public record DokumentproduksjonsInfo(String dokgenMalnavn, String dokumentKategoriKode, String journalføringsTittel, Map<VedleggTyper, String> vedleggsTitler) {
}
