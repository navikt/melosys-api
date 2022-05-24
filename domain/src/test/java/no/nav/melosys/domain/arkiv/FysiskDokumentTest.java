package no.nav.melosys.domain.arkiv;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FysiskDokumentTest {

    @Test
    void lagFysiskDokumentListeFraVedlegg_lagerFysiskDokumentMedKorrektInformasjon() {
        var vedlegg1Innhold = new byte[]{1, 2, 3};
        var vedlegg2Innhold = new byte[]{4, 5, 6};
        Vedlegg vedlegg1 = new Vedlegg(vedlegg1Innhold, "tittel for vedlegg1");
        Vedlegg vedlegg2 = new Vedlegg(vedlegg2Innhold, "tittel for vedlegg2");
        JournalpostBestilling journalpostBestilling = new JournalpostBestilling.Builder()
            .medBrevkode("brevkode y0")
            .medDokumentKategori("kategory y0").build();


        List<FysiskDokument> fysiskeDokumenter = FysiskDokument.lagFysiskDokumentListeFraVedlegg(journalpostBestilling,
            Arrays.asList(vedlegg1, vedlegg2));

        assertThat(fysiskeDokumenter).hasSize(2).extracting(
            FysiskDokument::getTittel,
            FysiskDokument::getBrevkode,
            FysiskDokument::getDokumentKategori,
            fysiskDokument -> fysiskDokument.getDokumentVarianter().get(0).getData()
        ).containsExactlyInAnyOrder(
            Tuple.tuple("tittel for vedlegg1", "brevkode y0", "kategory y0", vedlegg1Innhold),
            Tuple.tuple("tittel for vedlegg2", "brevkode y0", "kategory y0", vedlegg2Innhold)
        );
    }
}
