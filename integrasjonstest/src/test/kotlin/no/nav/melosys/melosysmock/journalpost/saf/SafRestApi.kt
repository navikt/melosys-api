package no.nav.melosys.melosysmock.journalpost.saf

import no.nav.melosys.melosysmock.journalpost.JournalpostRepo
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/rest")
class SafRestApi {
    @GetMapping("hentdokument/{journalpostId}/{dokumentInfoId}/{variantFormat}")
    fun hentDokument(
        @PathVariable journalpostId: String,
        @PathVariable dokumentInfoId: String,
        @PathVariable variantFormat: String
    ): ByteArray =
        JournalpostRepo.repo[journalpostId]
            ?.let { journalpost ->
                journalpost.dokumentModellList
                    .find { it.dokumentId == dokumentInfoId }
                    ?.let {
                        it.dokumentVarianter
                            ?.find { variant ->
                                variant.variantFormat.toString() == variantFormat
                            }?.dokumentInnhold
                            ?: throw NoSuchElementException("Finner ikke dokumentvariant $variantFormat for dokument $dokumentInfoId")
                    }
                    ?: throw NoSuchElementException("Finner ikke dokument $dokumentInfoId for journalpost $journalpostId")
            } ?: throw NoSuchElementException("Finner ikke journalpost $journalpostId")
}
