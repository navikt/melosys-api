package no.nav.melosys.domain.arkiv;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FysiskDokumentTest {

    @Test
    void lagFysiskDokumentFraVedlegg_lagerFysiskDokumentMedKorrektInformasjon() {
        byte[] vedlegg1 = new byte[]{1, 2, 3};
        byte[] vedlegg2 = new byte[]{4, 5, 6};
        JournalpostBestilling journalpostBestilling = new JournalpostBestilling.Builder().medTittel("tittel y0")
            .medBrevkode("brevkode y0")
            .medDokumentKategori("kategory y0").build();


        List<FysiskDokument> fysiskeDokumenter = FysiskDokument.lagFysiskDokumentFraVedlegg(journalpostBestilling,
            Arrays.asList(vedlegg1, vedlegg2));

        assertThat(fysiskeDokumenter).hasSize(2).extracting(
            FysiskDokument::getTittel,
            FysiskDokument::getBrevkode,
            FysiskDokument::getDokumentKategori,
            fysiskDokument -> fysiskDokument.getDokumentVarianter().get(0).getData()
        ).containsExactlyInAnyOrder(Tuple.tuple("tittel y0", "brevkode y0", "kategory y0", vedlegg1),
            Tuple.tuple("tittel y0", "brevkode y0", "kategory y0", vedlegg2));
    }
}
