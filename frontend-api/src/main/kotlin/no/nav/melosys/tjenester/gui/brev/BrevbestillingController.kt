package no.nav.melosys.tjenester.gui.brev

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.nav.melosys.domain.brev.NorskMyndighet
import no.nav.melosys.domain.brev.StandardvedleggType
import no.nav.melosys.service.brev.BrevbestillingFasade
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.brev.*
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.annotation.RequestScope

@Protected
@RestController
@RequestMapping("/dokumenter/v2")
@Api(tags = ["dokumenterv2"])
@RequestScope
class BrevbestillingController(
    private val brevbestillingFasade: BrevbestillingFasade,
    private val brevmalListeBygger: BrevmalListeBygger,
    private val aksesskontroll: Aksesskontroll
) {
    @GetMapping(value = ["/tilgjengelige-maler/{behandlingID}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiOperation(
        value = "Henter alle tilgjengelige brevmaler for en behandling",
        response = BrevmalResponse::class,
        responseContainer = "List"
    )
    fun hentTilgjengeligeMaler(@PathVariable behandlingID: Long): List<BrevmalResponse> {
        aksesskontroll.autoriser(behandlingID)

        return brevmalListeBygger.byggBrevmalDtoListe(behandlingID)
    }

    @PostMapping(value = ["/mulige-mottakere/{behandlingID}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiOperation(value = "Henter alle mulige mottakere for valgt dokumenttype, og organisasjonsnummer dersom hovedmottaker ikke er bruker")
    fun hentMuligeBrevmottakere(
        @PathVariable behandlingID: Long,
        @RequestBody hentMuligeBrevmottakereRequest: HentMuligeBrevmottakereRequest
    ): HentMuligeBrevmottakereResponse {
        aksesskontroll.autoriser(behandlingID)

        val hentMuligeMottakereRequestDto = hentMuligeBrevmottakereRequest.tilHentMuligeBrevmottakereRequestDto(behandlingID)
        return HentMuligeBrevmottakereResponse.av(brevbestillingFasade.hentMuligeMottakere(hentMuligeMottakereRequestDto))
    }

    @GetMapping(value = ["/standardvedlegg"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiOperation(
        value = "Henter alle tilgjengelige standardvedlegg",
        response = StandardvedleggType::class,
        responseContainer = "List"
    )
    fun hentStandardvedlegg(): List<StandardbrevDto> {
        return StandardvedleggType.values().map {
            StandardbrevDto(it, it.frontendTittel, it.tittel)
        }
    }

    @PostMapping(
        value = ["pdf/brev/utkast/{behandlingID}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_PDF_VALUE]
    )
    @ApiOperation(value = "Produser utkast")
    fun produserUtkast(
        @PathVariable behandlingID: Long,
        @RequestBody brevbestillingRequest: BrevbestillingRequest
    ): ResponseEntity<ByteArray> {
        aksesskontroll.autoriser(behandlingID)

        val brevbestillingDto = brevbestillingRequest.tilBrevbestillingDto()
        val pdfInBytes = brevbestillingFasade.produserUtkast(behandlingID, brevbestillingDto)
        return ResponseEntity(pdfInBytes, genPdfHeaders("utkast_$behandlingID"), HttpStatus.OK)
    }

    @GetMapping("pdf/utkast/standardvedlegg/{standardvedleggType}")
    @ApiOperation(value = "Produser standardvedlegg gjennom melosys-dokgen")
    fun hentStandardvedleggUtkast(
        @PathVariable("standardvedleggType") standardvedleggType: StandardvedleggType
    ): ResponseEntity<ByteArray> {
        val pdfInBytes = brevbestillingFasade.produserStandardvedleggPdf(standardvedleggType)
        return ResponseEntity(pdfInBytes, genPdfHeaders("standardvedlegg_${standardvedleggType.tittel}"), HttpStatus.OK)
    }

    @PostMapping("opprett/{behandlingID}")
    @ApiOperation(value = "Produser brev gjennom melosys-dokgen")
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
    @ApiOperation(value = "Henter alle mulige mottakere for valgte norske myndigheter")
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
    @ApiOperation(
        value = "Henter alle tilgjengelige norske myndigheter",
        response = NorskMyndighet::class,
        responseContainer = "List"
    )
    fun hentTilgjengeligeNorskeMyndigheter(): List<NorskMyndighet> = brevbestillingFasade.hentTilgjengeligeNorskeMyndigheter()

    private fun genPdfHeaders(navn: String): HttpHeaders =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_PDF
            contentDisposition = ContentDisposition.builder("inline").filename("$navn.pdf").build()
            cacheControl = "must-revalidate, post-check=0, pre-check=0"
        }
}
