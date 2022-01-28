package no.nav.melosys.service.dokument;

import no.nav.melosys.service.dokument.brev.mapper.DokumentproduksjonsInfoMapper;

import java.util.Map;

public record DokumentproduksjonsInfo(String dokgenMalnavn, String dokumentKategoriKode, String journalføringsTittel, Map<DokumentproduksjonsInfoMapper.VedleggsTyper, String> vedleggsTitler) {
}
