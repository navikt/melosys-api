package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.types.vedlegg.VedleggFiltype
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.junit.jupiter.api.Test

class BildeTilPdfKonvertererTest {

    private val konverterer = BildeTilPdfKonverterer()

    @Test
    fun `PDF-vedlegg sendes uendret gjennom`() {
        val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46) // "%PDF"

        val resultat = konverterer.konverterTilPdfHvisBilde(pdfBytes, VedleggFiltype.PDF)

        resultat shouldBe pdfBytes
    }

    @Test
    fun `JPEG-vedlegg konverteres til en gyldig PDF`() {
        val resultat = konverterer.konverterTilPdfHvisBilde(lagBildeBytes("jpg"), VedleggFiltype.JPEG)

        assertErGyldigPdfMedEnSide(resultat)
    }

    @Test
    fun `PNG-vedlegg konverteres til en gyldig PDF`() {
        val resultat = konverterer.konverterTilPdfHvisBilde(lagBildeBytes("png"), VedleggFiltype.PNG)

        assertErGyldigPdfMedEnSide(resultat)
    }

    @Test
    fun `konvertert PDF har A4-sidestoerrelse`() {
        val resultat = konverterer.konverterTilPdfHvisBilde(lagBildeBytes("jpg"), VedleggFiltype.JPEG)

        Loader.loadPDF(resultat).use { dokument ->
            val side = dokument.getPage(0)
            side.mediaBox.width shouldBe PDRectangle.A4.width
            side.mediaBox.height shouldBe PDRectangle.A4.height
        }
    }

    private fun assertErGyldigPdfMedEnSide(bytes: ByteArray) {
        Loader.loadPDF(bytes).use { dokument ->
            dokument.numberOfPages shouldBe 1
        }
    }
}

