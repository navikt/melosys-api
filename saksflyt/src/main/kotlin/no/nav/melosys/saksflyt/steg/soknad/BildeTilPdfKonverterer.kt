package no.nav.melosys.saksflyt.steg.soknad

import no.nav.melosys.skjema.types.vedlegg.VedleggFiltype
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
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
        val bilde = ByteArrayInputStream(innhold).use { ImageIO.read(it) }
            ?: throw IllegalArgumentException(
                "Kunne ikke lese bildevedlegg av type $filtype (bytes=${innhold.size}) for konvertering til PDF"
            )

        PDDocument().use { dokument ->
            val pdImage = when (filtype) {
                VedleggFiltype.JPEG -> JPEGFactory.createFromImage(dokument, bilde)
                else -> LosslessFactory.createFromImage(dokument, bilde)
            }

            val side = PDPage(
                PDRectangle(
                    pdImage.width + PDF_MARGIN * 2,
                    pdImage.height + PDF_MARGIN * 2
                )
            )
            dokument.addPage(side)

            PDPageContentStream(dokument, side).use { innholdsstrom ->
                innholdsstrom.drawImage(pdImage, PDF_MARGIN, PDF_MARGIN)
            }

            return ByteArrayOutputStream().use { ut ->
                dokument.save(ut)
                ut.toByteArray()
            }
        }
    }

    private companion object {
        const val PDF_MARGIN = 20.0F
    }
}

