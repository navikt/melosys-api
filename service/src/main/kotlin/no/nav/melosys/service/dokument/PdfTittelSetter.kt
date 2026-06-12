package no.nav.melosys.service.dokument

import mu.KotlinLogging
import org.apache.pdfbox.Loader
import java.io.ByteArrayOutputStream

object PdfTittelSetter {
    private val log = KotlinLogging.logger { }

    /**
     * Setter PDF /Title-metadata.
     *
     * @param bevarOriginaleBytes true for dokumenter hentet fra arkivet (Joark): bruker
     *   inkrementell lagring som ikke skriver om de originale bytene (i motsetning til full
     *   lagring). Det signerte byte-området i en evt. digital signatur forblir dermed intakt,
     *   men endring-etter-signering kan likevel flagges av strenge validatorer, og PDF/A-
     *   konformitet for den nye revisjonen er ikke garantert. false (default) for PDF-er vi
     *   selv genererer, der en ren full lagring er greit.
     */
    @JvmStatic
    @JvmOverloads
    fun settTittel(pdfBytes: ByteArray, tittel: String, bevarOriginaleBytes: Boolean = false): ByteArray {
        if (tittel.isBlank()) return pdfBytes

        return try {
            Loader.loadPDF(pdfBytes).use { pdf ->
                if (pdf.documentInformation.title == tittel) {
                    pdfBytes
                } else {
                    pdf.documentInformation.title = tittel
                    val baos = ByteArrayOutputStream()
                    if (bevarOriginaleBytes) pdf.saveIncremental(baos) else pdf.save(baos)
                    baos.toByteArray()
                }
            }
        } catch (e: Exception) {
            // Bevisst bred fangst: tittel-setting er en ikke-kritisk metadata-forbedring som
            // aldri skal velte dokumentvisningen. Exceptionen logges, og originale bytes returneres.
            log.warn(e) { "Kunne ikke sette PDF Title-metadata, returnerer originale bytes" }
            pdfBytes
        }
    }
}
