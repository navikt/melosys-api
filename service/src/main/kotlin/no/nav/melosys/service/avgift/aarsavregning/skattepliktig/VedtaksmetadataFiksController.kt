package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import mu.KotlinLogging
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger { }

private val SAKSNUMMER_FORMAT = Regex("^MEL-\\d+$")

@Protected
@RestController
@RequestMapping("/admin/aarsavregninger/saker/skattepliktige")
class VedtaksmetadataFiksController(
    private val vedtaksmetadataFiksService: VedtaksmetadataFiksService
) {

    @Operation(
        summary = "Sett inn manglende vedtaksmetadata for oppgitte saker (datafiks)",
        description = "Enkelte avsluttede behandlinger mangler rad i vedtak_metadata, og da feiler " +
            "årsavregningen for hele saken. Dette endepunktet setter inn de manglende radene. " +
            "Med skarp=false (default) endres ingenting — svaret viser hvilke rader som ville blitt " +
            "satt inn. Med skarp=true settes radene inn: da må saksnummer angis, og antall rader kan " +
            "ikke overstige maksAntallRader (default 10). Kjøringen kan trygt gjentas (allerede " +
            "fiksede rader settes ikke inn på nytt), og radene merkes slik at de kan slettes igjen " +
            "med /vedtaksmetadata-fiks/angre. " +
            "Vedtaksdatoen som settes er en tilnærming (behandlingsresultatets endret_dato). Blir " +
            "den tilnærmede datoen den nyeste i saken, kan den endre hvilken behandling " +
            "årsavregningen henter avgiftsgrunnlaget fra — da avvises kjøringen (trengerGodkjenning " +
            "i svaret er true). Sett i så fall riktig vedtaksdato manuelt, eller legg saksnummeret i " +
            "tillatSorteringsendring for å godkjenne endringen for akkurat den saken."
    )
    @PostMapping("/vedtaksmetadata-fiks")
    fun vedtaksmetadataFiks(
        @RequestBody
        @Parameter(description = "Saksnummer, skarp-flagg og valgfritt maksAntallRader")
        request: VedtaksmetadataFiksRequest
    ): ResponseEntity<Any> {
        // distinct() før valider(): duplikater skal ikke telle mot MAKS_ANTALL_SAKER
        val saksnummer = (if (request.skarp) request.saksnummer else request.saksnummer.ifEmpty { VedtaksmetadataFiksService.STANDARD_SAKER })
            .distinct()
        val tillatSorteringsendring = request.tillatSorteringsendring.distinct()

        valider(saksnummer)?.let { return it }
        valider(tillatSorteringsendring)?.let { return it }

        log.info {
            "Datafiks vedtaksmetadata (${if (request.skarp) "SKARP" else "PREVIEW"}) for saker $saksnummer"
        }

        return try {
            val resultat = if (request.skarp) {
                vedtaksmetadataFiksService.utfør(
                    saksnummer,
                    request.maksAntallRader,
                    tillatSorteringsendring,
                )
            } else {
                vedtaksmetadataFiksService.forhåndsvis(saksnummer, tillatSorteringsendring)
            }
            ResponseEntity.ok(resultat)
        } catch (e: VedtaksmetadataFiksAvvist) {
            log.warn { "Datafiks vedtaksmetadata avvist: ${e.message}" }
            ResponseEntity.badRequest().body(mapOf("feil" to e.message))
        }
    }

    @Operation(
        summary = "Slett rader som datafiksen har satt inn",
        description = "Med skarp=false (default) endres ingenting — svaret viser hva som ville blitt " +
            "slettet. Tom saksnummer-liste betyr alle rader fiksen har satt inn, uansett sak; skarp " +
            "sletting uten saksnummer krever derfor også bekreftAlle=true. " +
            "Rader som er endret etter innsettingen slettes aldri — de telles i antallSomIkkeKanAngres."
    )
    @PostMapping("/vedtaksmetadata-fiks/angre")
    fun angreVedtaksmetadataFiks(
        @RequestBody(required = false)
        @Parameter(description = "Valgfritt saksnummer-scope og skarp-flagg")
        request: VedtaksmetadataAngreRequest?
    ): ResponseEntity<Any> {
        val angreRequest = request ?: VedtaksmetadataAngreRequest()
        val saksnummer = angreRequest.saksnummer.distinct()

        valider(saksnummer)?.let { return it }

        log.info {
            "Angre datafiks vedtaksmetadata (${if (angreRequest.skarp) "SKARP" else "PREVIEW"}), " +
                "scope=${saksnummer.ifEmpty { listOf("ALLE") }}"
        }

        return try {
            ResponseEntity.ok(
                vedtaksmetadataFiksService.angre(
                    saksnummer,
                    angreRequest.skarp,
                    angreRequest.bekreftAlle,
                )
            )
        } catch (e: VedtaksmetadataFiksAvvist) {
            log.warn { "Angre datafiks vedtaksmetadata avvist: ${e.message}" }
            ResponseEntity.badRequest().body(mapOf("feil" to e.message))
        }
    }

    private fun valider(saksnummer: List<String>): ResponseEntity<Any>? {
        val ugyldige = saksnummer.filterNot { SAKSNUMMER_FORMAT.matches(it) }
        if (ugyldige.isNotEmpty()) {
            return ResponseEntity.badRequest().body(
                mapOf("feil" to "Ugyldig saksnummerformat, forventer MEL-<tall>", "ugyldige" to ugyldige)
            )
        }
        if (saksnummer.size > VedtaksmetadataFiksService.MAKS_ANTALL_SAKER) {
            return ResponseEntity.badRequest().body(
                mapOf(
                    "feil" to "For mange saksnummer i ett kall",
                    "antall" to saksnummer.size,
                    "maks" to VedtaksmetadataFiksService.MAKS_ANTALL_SAKER
                )
            )
        }
        return null
    }
}

data class VedtaksmetadataFiksRequest(
    /** Påkrevd ved skarp = true. Tom liste i forhåndsvisning bruker [VedtaksmetadataFiksService.STANDARD_SAKER]. */
    val saksnummer: List<String> = emptyList(),
    val skarp: Boolean = false,
    val maksAntallRader: Int = VedtaksmetadataFiksService.STANDARD_MAKS_ANTALL_RADER,
    /** Saker der det er godkjent at den tilnærmede vedtaksdatoen blir nyeste i saken. Liste, ikke flagg: gjelder kun disse. */
    val tillatSorteringsendring: List<String> = emptyList(),
)

data class VedtaksmetadataAngreRequest(
    /** Tom liste = alle rader fiksen har satt inn, uansett sak. Krever [bekreftAlle] ved skarp. */
    val saksnummer: List<String> = emptyList(),
    val skarp: Boolean = false,
    /** Påkrevd ved skarp sletting uten saksnummer — et glemt felt skal ikke slette alt. */
    val bekreftAlle: Boolean = false,
)
