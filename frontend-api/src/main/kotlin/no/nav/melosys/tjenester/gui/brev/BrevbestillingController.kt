package no.nav.melosys.tjenester.gui.brev

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.domain.brev.NorskMyndighet
import no.nav.melosys.domain.brev.StandardvedleggType
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.service.brev.BrevbestillingFasade
import no.nav.melosys.service.dokument.PdfTittelSetter
import no.nav.melosys.service.dokument.brev.mapper.hentStandardvedlegg
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.Filnavn
import no.nav.melosys.tjenester.gui.dto.brev.*
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.annotation.RequestScope
import java.nio.charset.StandardCharsets

@Protected
@RestController
@RequestMapping("/dokumenter/v2")
@Tag(name = "dokumenterv2")
@RequestScope
class BrevbestillingController(
    private val brevbestillingFasade: BrevbestillingFasade,
    private val brevmalListeBygger: BrevmalListeBygger,
    private val aksesskontroll: Aksesskontroll
) {
    @GetMapping(value = ["/tilgjengelige-maler/{behandlingID}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Henter alle tilgjengelige brevmaler for en behandling"
    )
    fun hentTilgjengeligeMaler(@PathVariable behandlingID: Long): List<BrevmalResponse> {
        aksesskontroll.autoriser(behandlingID)

        return brevmalListeBygger.byggBrevmalDtoListe(behandlingID)
    }

    @PostMapping(value = ["/mulige-mottakere/{behandlingID}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Henter alle mulige mottakere for valgt dokumenttype, og organisasjonsnummer dersom hovedmottaker ikke er bruker")
    fun hentMuligeBrevmottakere(
        @PathVariable behandlingID: Long,
        @RequestBody hentMuligeBrevmottakereRequest: HentMuligeBrevmottakereRequest
    ): HentMuligeBrevmottakereResponse {
        aksesskontroll.autoriser(behandlingID)

        val hentMuligeMottakereRequestDto = hentMuligeBrevmottakereRequest.tilHentMuligeBrevmottakereRequestDto(behandlingID)
        return HentMuligeBrevmottakereResponse.av(brevbestillingFasade.hentMuligeMottakere(hentMuligeMottakereRequestDto))
    }

    @GetMapping(value = ["/standardvedlegg"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Henter alle tilgjengelige standardvedlegg"
    )
    fun hentStandardvedlegg(): List<StandardvedleggDto> {
        return StandardvedleggType.values().map {
            StandardvedleggDto(it, it.frontendTittel, it.journalføringstittel)
        }
    }

    @GetMapping(value = ["/standardvedlegg/{produserbaredokumentType}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Henter standardvedlegg for valgt dokumenttype"
    )
    fun hentStandardvedleggForDokumenttype(@PathVariable produserbaredokumentType: Produserbaredokumenter): List<StandardvedleggDto> {
        return produserbaredokumentType.hentStandardvedlegg().map {
            StandardvedleggDto(it, it.frontendTittel, it.journalføringstittel)
        }
    }

    @PostMapping(
        value = ["pdf/brev/utkast/{behandlingID}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_PDF_VALUE]
    )
    @Operation(summary = "Produser utkast")
    fun produserUtkast(
        @PathVariable behandlingID: Long,
        @RequestBody brevbestillingRequest: BrevbestillingRequest
    ): ResponseEntity<ByteArray> {
        aksesskontroll.autoriser(behandlingID)

        val brevbestillingDto = brevbestillingRequest.tilBrevbestillingDto()
        val pdfInBytes = brevbestillingFasade.produserUtkast(behandlingID, brevbestillingDto)
        val tittel = brevbestillingDto.produserbardokument?.beskrivelse ?: "Brev-utkast"
        val pdfMedTittel = PdfTittelSetter.settTittel(pdfInBytes, tittel)
        return ResponseEntity(pdfMedTittel, genPdfHeaders(tittel), HttpStatus.OK)
    }

    @GetMapping("pdf/utkast/standardvedlegg/{standardvedleggType}")
    @Operation(summary = "Produser standardvedlegg gjennom melosys-dokgen")
    fun hentStandardvedleggUtkast(
        @PathVariable("standardvedleggType") standardvedleggType: StandardvedleggType
    ): ResponseEntity<ByteArray> {
        val pdfInBytes = brevbestillingFasade.produserStandardvedleggPdf(standardvedleggType)
        val tittel = standardvedleggType.journalføringstittel
        val pdfMedTittel = PdfTittelSetter.settTittel(pdfInBytes, tittel)
        return ResponseEntity(pdfMedTittel, genPdfHeaders("standardvedlegg_$tittel"), HttpStatus.OK)
    }

    @PostMapping("opprett/{behandlingID}")
    @Operation(summary = "Produser brev gjennom melosys-dokgen")
    fun produserBrev(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody brevbestillingRequest: BrevbestillingRequest
    ) {
        aksesskontroll.autoriser(behandlingID)

        val brevbestillingDto = brevbestillingRequest.tilBrevbestillingDto()
        brevbestillingFasade.produserBrev(behandlingID, brevbestillingDto)
    }

    @PostMapping(
        value = ["/mulige-mottakere-norske-myndigheter/{behandlingID}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Henter alle mulige mottakere for valgte norske myndigheter")
    fun hentMuligeBrevmottakereNorskMyndighet(
        @PathVariable behandlingID: Long,
        @RequestBody hentMuligeBrevmottakereNorskMyndighetRequest: HentMuligeBrevmottakereNorskMyndighetRequest
    ): List<MuligBrevmottaker> {
        aksesskontroll.autoriser(behandlingID)

        val muligeBrevmottakere =
            brevbestillingFasade.hentMuligeBrevmottakereNorskMyndighet(hentMuligeBrevmottakereNorskMyndighetRequest.orgnrNorskMyndighet)
        return muligeBrevmottakere.map(MuligBrevmottaker::av)
    }

    @GetMapping(value = ["/tilgjengelige-norske-myndigheter"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Henter alle tilgjengelige norske myndigheter"
    )
    fun hentTilgjengeligeNorskeMyndigheter(): List<NorskMyndighet> = brevbestillingFasade.hentTilgjengeligeNorskeMyndigheter()

    private fun genPdfHeaders(navn: String): HttpHeaders =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_PDF
            contentDisposition = ContentDisposition.builder("inline")
                .filename("${Filnavn.saner(navn)}.pdf", StandardCharsets.UTF_8)
                .build()
            cacheControl = "must-revalidate, post-check=0, pre-check=0"
        }
}
