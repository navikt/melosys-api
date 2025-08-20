package no.nav.melosys.domain.arkiv

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class FysiskDokumentTest {

    @Test
    fun lagFysiskDokumentListeFraVedlegg_lagerFysiskDokumentMedKorrektInformasjon() {
        val vedlegg1Innhold = byteArrayOf(1, 2, 3)
        val vedlegg2Innhold = byteArrayOf(4, 5, 6)
        val vedlegg1 = Vedlegg(vedlegg1Innhold, "tittel for vedlegg1")
        val vedlegg2 = Vedlegg(vedlegg2Innhold, "tittel for vedlegg2")
        val journalpostBestilling = JournalpostBestilling.Builder()
            .medBrevkode("brevkode y0")
            .medDokumentKategori("kategory y0").build()


        val fysiskeDokumenter = FysiskDokument.lagFysiskDokumentListeFraVedlegg(
            journalpostBestilling,
            listOf(vedlegg1, vedlegg2)
        )


        fysiskeDokumenter shouldHaveSize 2

        // Verify first document
        val førsteDokument = fysiskeDokumenter.find { it.tittel == "tittel for vedlegg1" }!!
        førsteDokument.run {
            tittel shouldBe "tittel for vedlegg1"
            brevkode shouldBe "brevkode y0"
            dokumentKategori shouldBe "kategory y0"
            dokumentVarianter[0].data shouldBe vedlegg1Innhold
        }

        // Verify second document
        val andreDokument = fysiskeDokumenter.find { it.tittel == "tittel for vedlegg2" }!!
        andreDokument.run {
            tittel shouldBe "tittel for vedlegg2"
            brevkode shouldBe "brevkode y0"
            dokumentKategori shouldBe "kategory y0"
            dokumentVarianter[0].data shouldBe vedlegg2Innhold
        }
    }
}
