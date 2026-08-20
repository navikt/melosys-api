package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import mu.KotlinLogging
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger { }

private val SAKSNUMMER_FORMAT = Regex("^MEL-\\d+$")

@Protected
@RestController
@RequestMapping("/admin/aarsavregninger/saker/skattepliktige")
class SkattepliktigeAarsavregningDryrunController(
    private val skattepliktigeAarsavregningDryrunService: SkattepliktigeAarsavregningDryrunService,
    private val vedtaksmetadataFiksService: VedtaksmetadataFiksService
) {

    @Operation(
        summary = "Kjøring av skattehendelser (dryrun eller skarpt)",
        description = "Prosesserer skattehendelser for skattepliktige. " +
            "Med skarp=false (default) er det en simulering — ingen prosessinstanser opprettes. " +
            "Med skarp=true opprettes faktiske AARSAVREGNING-prosessinstanser, kappet av maksAntall. " +
            "Bruk /status for fremdrift og /rapport for detaljert resultat."
    )
    @PostMapping("/run")
    fun run(
        @RequestBody
        @Parameter(description = "Liste med skattehendelser, skarp-flagg, og valgfritt maksAntall")
        request: SkattehendelseRunRequest
    ): ResponseEntity<Map<String, Any?>> {
        val modus = if (request.skarp) "SKARP" else "DRYRUN"
        log.info {
            "Starter $modus for ${request.skattehendelser.size} skattehendelser, maksAntall=${request.maksAntall}"
        }

        skattepliktigeAarsavregningDryrunService.prosesserSkattehendelserAsynkront(
            request.skattehendelser,
            request.skarp,
            request.maksAntall,
        )

        return ResponseEntity.ok(
            mapOf(
                "melding" to "$modus startet",
                "skarp" to request.skarp,
                "maksAntall" to request.maksAntall,
                "antallHendelser" to request.skattehendelser.size,
                "statusEndpoint" to "/admin/aarsavregninger/saker/skattepliktige/status",
                "rapportEndpoint" to "/admin/aarsavregninger/saker/skattepliktige/rapport"
            )
        )
    }

    @Operation(summary = "Hent status for pågående eller siste dryrun")
    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any?>> =
        ResponseEntity(skattepliktigeAarsavregningDryrunService.status(), HttpStatus.OK)

    @Operation(summary = "Hent detaljert rapport med alle saker fra siste dryrun")
    @GetMapping("/rapport", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun rapport(): ResponseEntity<String> =
        ResponseEntity(skattepliktigeAarsavregningDryrunService.rapportJsonString(), HttpStatus.OK)

    @Operation(
        summary = "Datafiks: sett inn manglende vedtaksmetadata (MELOSYS-8174, Q4a/Q4b)",
        description = "Med skarp=false (default) er dette Q4a — en read-only forhåndsvisning av nøyaktig " +
            "hvilke rader som ville blitt satt inn, med de tre sakene i fiksplanen som default. " +
            "Med skarp=true er det Q4b, som endrer data: da må saksnummer angis eksplisitt, " +
            "antall rader må ligge innenfor maksAntallRader (default 10), og alle kandidater må ha en " +
            "beh_type vi kan utlede vedtakstype fra. Innsettingen er idempotent, og alle rader merkes " +
            "MELOSYS-8174-PATCH slik at de kan rulles tilbake med /vedtaksmetadata-fiks/angre."
    )
    @PostMapping("/vedtaksmetadata-fiks")
    fun vedtaksmetadataFiks(
        @RequestBody
        @Parameter(description = "Saksnummer, skarp-flagg og valgfritt maksAntallRader")
        request: VedtaksmetadataFiksRequest
    ): ResponseEntity<Any> {
        val saksnummer = if (request.skarp) request.saksnummer else request.saksnummer.ifEmpty { VedtaksmetadataFiksService.STANDARD_SAKER }

        valider(saksnummer)?.let { return it }

        log.info {
            "Datafiks vedtaksmetadata (${if (request.skarp) "SKARP" else "PREVIEW"}) for saker $saksnummer"
        }

        return try {
            val resultat = if (request.skarp) {
                vedtaksmetadataFiksService.utfoer(saksnummer, request.maksAntallRader)
            } else {
                vedtaksmetadataFiksService.forhaandsvis(saksnummer)
            }
            ResponseEntity.ok(resultat)
        } catch (e: VedtaksmetadataFiksAvvist) {
            log.warn { "Datafiks vedtaksmetadata avvist: ${e.message}" }
            ResponseEntity.badRequest().body(mapOf("feil" to e.message))
        }
    }

    @Operation(
        summary = "Angre datafiksen: slett rader merket MELOSYS-8174-PATCH",
        description = "Med skarp=false (default) vises kun hva som ville blitt slettet. " +
            "Tom saksnummer-liste betyr alle markerte rader; angi saksnummer for å angre én sak om gangen. " +
            "Rader der markøren er overskrevet av en senere endring (endret_av != MELOSYS-8174-PATCH) " +
            "røres aldri — de telles i antallEndretEtterpaa."
    )
    @PostMapping("/vedtaksmetadata-fiks/angre")
    fun angreVedtaksmetadataFiks(
        @RequestBody(required = false)
        @Parameter(description = "Valgfritt saksnummer-scope og skarp-flagg")
        request: VedtaksmetadataAngreRequest?
    ): ResponseEntity<Any> {
        val angreRequest = request ?: VedtaksmetadataAngreRequest()

        valider(angreRequest.saksnummer)?.let { return it }

        log.info {
            "Angre datafiks vedtaksmetadata (${if (angreRequest.skarp) "SKARP" else "PREVIEW"}), " +
                "scope=${angreRequest.saksnummer.ifEmpty { listOf("ALLE") }}"
        }

        return ResponseEntity.ok(vedtaksmetadataFiksService.angre(angreRequest.saksnummer, angreRequest.skarp))
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

data class SkattehendelseRunRequest(
    val skattehendelser: List<SkattehendelseDryrunItem>,
    val skarp: Boolean = false,
    val maksAntall: Int? = null,
)

data class VedtaksmetadataFiksRequest(
    /** Påkrevd ved skarp = true. I preview brukes de tre sakene fra fiksplanen hvis lista er tom. */
    val saksnummer: List<String> = emptyList(),
    val skarp: Boolean = false,
    /** Sikkerhetssele: skarp kjøring avvises hvis den ville satt inn flere rader enn dette. */
    val maksAntallRader: Int = VedtaksmetadataFiksService.DEFAULT_MAKS_ANTALL_RADER,
)

data class VedtaksmetadataAngreRequest(
    /** Tom liste = alle rader merket MELOSYS-8174-PATCH, uansett sak. */
    val saksnummer: List<String> = emptyList(),
    val skarp: Boolean = false,
)
