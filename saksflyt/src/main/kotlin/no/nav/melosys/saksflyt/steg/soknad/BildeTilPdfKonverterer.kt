package no.nav.melosys.saksflyt.steg.soknad

import no.nav.melosys.skjema.types.vedlegg.VedleggFiltype
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Konverterer bildevedlegg (JPEG/PNG) til PDF før journalføring, fordi Joark kun
 * godtar PDF/PDFA/XLSX som ARKIV-variant. Bildet tegnes inn på en PDF-side.
 */
@Component
class BildeTilPdfKonverterer {

    fun konverterTilPdfHvisBilde(innhold: ByteArray, filtype: VedleggFiltype): ByteArray =
        when (filtype) {
            VedleggFiltype.PDF -> innhold
            VedleggFiltype.JPEG, VedleggFiltype.PNG -> konverterBildeTilPdf(innhold, filtype)
        }

    private fun konverterBildeTilPdf(innhold: ByteArray, filtype: VedleggFiltype): ByteArray {
        PDDocument().use { dokument ->
            val pdImage = when (filtype) {
                VedleggFiltype.JPEG -> embedJpeg(dokument, innhold)
                else -> LosslessFactory.createFromImage(dokument, lesBilde(innhold, filtype))
            }

            val side = PDPage(PDRectangle.A4)
            dokument.addPage(side)

            val tilgjengeligBredde = PDRectangle.A4.width - PDF_MARGIN * 2
            val tilgjengeligHoyde = PDRectangle.A4.height - PDF_MARGIN * 2
            val skala = minOf(
                tilgjengeligBredde / pdImage.width,
                tilgjengeligHoyde / pdImage.height,
                1.0F
            )
            val bredde = pdImage.width * skala
            val hoyde = pdImage.height * skala
            val x = (PDRectangle.A4.width - bredde) / 2
            val y = (PDRectangle.A4.height - hoyde) / 2

            PDPageContentStream(dokument, side).use { innholdsstrom ->
                innholdsstrom.drawImage(pdImage, x, y, bredde, hoyde)
            }

            return ByteArrayOutputStream().use { ut ->
                dokument.save(ut)
                ut.toByteArray()
            }
        }
    }

    /**
     * Embedder original-JPEG-en uendret for å unngå dobbel komprimering. Faller tilbake til
     * ImageIO-dekoding for JPEG-er createFromStream ikke takler (f.eks. CMYK/progressive).
     */
    private fun embedJpeg(dokument: PDDocument, innhold: ByteArray): PDImageXObject =
        try {
            JPEGFactory.createFromStream(dokument, ByteArrayInputStream(innhold))
        } catch (e: Exception) {
            log.info("Kunne ikke embedde JPEG direkte, faller tilbake til ImageIO-dekoding", e)
            JPEGFactory.createFromImage(dokument, lesBilde(innhold, VedleggFiltype.JPEG))
        }

    private fun lesBilde(innhold: ByteArray, filtype: VedleggFiltype) =
        ByteArrayInputStream(innhold).use { ImageIO.read(it) }
            ?: throw IllegalArgumentException(
                "Kunne ikke lese bildevedlegg av type $filtype (bytes=${innhold.size}) for konvertering til PDF"
            )

    private companion object {
        private val log = LoggerFactory.getLogger(BildeTilPdfKonverterer::class.java)
        const val PDF_MARGIN = 20.0F
    }
}

