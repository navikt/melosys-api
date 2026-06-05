package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.types.vedlegg.VedleggFiltype
import org.apache.pdfbox.Loader
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

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
        val resultat = konverterer.konverterTilPdfHvisBilde(lagBilde("jpg"), VedleggFiltype.JPEG)

        assertErGyldigPdfMedEnSide(resultat)
    }

    @Test
    fun `PNG-vedlegg konverteres til en gyldig PDF`() {
        val resultat = konverterer.konverterTilPdfHvisBilde(lagBilde("png"), VedleggFiltype.PNG)

        assertErGyldigPdfMedEnSide(resultat)
    }

    private fun assertErGyldigPdfMedEnSide(bytes: ByteArray) {
        Loader.loadPDF(bytes).use { dokument ->
            dokument.numberOfPages shouldBe 1
        }
    }

    private fun lagBilde(format: String): ByteArray {
        val bilde = BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB)
        val grafikk = bilde.createGraphics()
        grafikk.color = Color.BLUE
        grafikk.fillRect(0, 0, 40, 30)
        grafikk.dispose()
        return ByteArrayOutputStream().use { ut ->
            ImageIO.write(bilde, format, ut)
            ut.toByteArray()
        }
    }
}

