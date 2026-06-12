package no.nav.melosys.service.dokument

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class PdfTittelSetterTest {

    @Test
    fun `settTittel skal sette PDF Title-metadata på byte-array`() {
        val pdfBytes = byggMinimalPdf()
        Loader.loadPDF(pdfBytes).use { pdf ->
            pdf.documentInformation.title shouldBe null
        }

        val resultat = PdfTittelSetter.settTittel(pdfBytes, "Attest A1")

        Loader.loadPDF(resultat).use { pdf ->
            pdf.documentInformation.title shouldBe "Attest A1"
        }
    }

    @Test
    fun `settTittel skal returnere uendrede bytes ved blank tittel`() {
        val pdfBytes = byggMinimalPdf()

        val resultat = PdfTittelSetter.settTittel(pdfBytes, " ")

        resultat shouldBe pdfBytes
    }

    @Test
    fun `settTittel skal returnere uendrede bytes hvis input ikke er gyldig PDF`() {
        val ikkePdf = "ikke en pdf".toByteArray()

        val resultat = PdfTittelSetter.settTittel(ikkePdf, "Attest A1")

        resultat shouldBe ikkePdf
    }

    @Test
    fun `settTittel med bevarOriginaleBytes skal sette tittel og beholde originale bytes`() {
        val pdfBytes = byggMinimalPdf()

        val resultat = PdfTittelSetter.settTittel(pdfBytes, "Arkivert dokument", bevarOriginaleBytes = true)

        Loader.loadPDF(resultat).use { pdf ->
            pdf.documentInformation.title shouldBe "Arkivert dokument"
        }
        resultat.copyOfRange(0, pdfBytes.size).contentEquals(pdfBytes) shouldBe true
        (resultat.size > pdfBytes.size) shouldBe true
    }

    @Test
    fun `settTittel skal returnere uendrede bytes naar tittelen allerede er riktig`() {
        val medTittel = PdfTittelSetter.settTittel(byggMinimalPdf(), "Allerede satt")

        val resultat = PdfTittelSetter.settTittel(medTittel, "Allerede satt")

        resultat shouldBeSameInstanceAs medTittel
    }

    private fun byggMinimalPdf(): ByteArray {
        val baos = ByteArrayOutputStream()
        PDDocument().use { pdf ->
            pdf.addPage(PDPage())
            pdf.save(baos)
        }
        return baos.toByteArray()
    }
}
