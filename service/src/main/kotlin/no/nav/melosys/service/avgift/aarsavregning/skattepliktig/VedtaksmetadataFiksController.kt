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

/**
 * Admin-endepunktene for datafiksen i MELOSYS-8174 — se [VedtaksmetadataFiksService] for hva feilen
 * er og hvorfor fiksen ser ut som den gjør.
 *
 * Endepunktene ligger permanent på master, ikke på en ops-branch: fiksen er idempotent, read-only
 * som default, og hardt selet (eksplisitt saksnummer, tak på antall rader, avvisning av ukjent
 * `beh_type`, sorteringssele, scoped angre). Samme feilklasse har dukket opp før — Flyway-patchen
 * `V7.6_04` var forrige runde av nøyaktig dette — og alternativet, å re-implementere fiksen hver
 * gang, er dyrere og farligere enn å la den stå ferdig reviewet.
 *
 * Basestien er årsavregningens skattepliktige admin-flate; kjøringsverktøyet (MELOSYS-8045) legger
 * seg på samme base med egne underliggende stier.
 */
@Protected
@RestController
@RequestMapping("/admin/aarsavregninger/saker/skattepliktige")
class VedtaksmetadataFiksController(
    private val vedtaksmetadataFiksService: VedtaksmetadataFiksService
) {

    @Operation(
        summary = "Datafiks: sett inn manglende vedtaksmetadata (MELOSYS-8174, Q4a/Q4b)",
        description = "Med skarp=false (default) er dette Q4a — en read-only forhåndsvisning av nøyaktig " +
            "hvilke rader som ville blitt satt inn, med de tre sakene i fiksplanen som default. " +
            "Med skarp=true er det Q4b, som endrer data: da må saksnummer angis eksplisitt, " +
            "antall rader må ligge innenfor maksAntallRader (default 10), og alle kandidater må ha en " +
            "beh_type vi kan utlede vedtakstype fra. Innsettingen er idempotent, og alle rader merkes " +
            "MELOSYS-8174-PATCH slik at de kan rulles tilbake med /vedtaksmetadata-fiks/angre. " +
            "VIKTIG: vedtak_dato settes til proxyen behandlingsresultat.endret_dato, og den datoen " +
            "styrer hvilken behandling ÅrsavregningService regner som nyest — altså hvor " +
            "avgiftsgrunnlaget hentes fra. Les sorteringspåvirkning i svaret: er patchenVinnerNyeste " +
            "true for en sak, avvises skarp kjøring: sett ekte vedtaksdato manuelt, eller list " +
            "saksnummeret i tillatSorteringsendring hvis endringen er vurdert — det kvitterer ut " +
            "saken, ikke selen, så de øvrige sakene i kallet er fortsatt beskyttet. " +
            "Merk at false ikke er et " +
            "frikjenn — sammenligningen er global maks mot global maks, mens den ekte utvelgelsen " +
            "først filtrerer på år og periodeoverlapp. Se ekteDatoer for hva patchen faktisk slår."
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
                vedtaksmetadataFiksService.utfør(
                    saksnummer,
                    request.maksAntallRader,
                    request.tillatSorteringsendring,
                )
            } else {
                vedtaksmetadataFiksService.forhåndsvis(saksnummer)
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
            "Skarp kjøring uten saksnummer krever bekreftAlle=true, fordi den ellers ruller tilbake " +
            "alle patch-rader i basen — også fikser fra tidligere kjøringer. " +
            "Rader der markøren er overskrevet av en senere endring (endret_av != MELOSYS-8174-PATCH) " +
            "røres aldri — de telles i antallEndretEtterpå. Det gjelder også rader der endret_av er " +
            "tømt: de kan ikke rulles tilbake automatisk, og telles i samme felt."
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

        return try {
            ResponseEntity.ok(
                vedtaksmetadataFiksService.angre(
                    angreRequest.saksnummer,
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
    /** Påkrevd ved skarp = true. I preview brukes de tre sakene fra fiksplanen hvis lista er tom. */
    val saksnummer: List<String> = emptyList(),
    val skarp: Boolean = false,
    /** Sikkerhetssele: skarp kjøring avvises hvis den ville satt inn flere rader enn dette. */
    val maksAntallRader: Int = VedtaksmetadataFiksService.STANDARD_MAKS_ANTALL_RADER,
    /**
     * Saksnummer der det er vurdert og ønsket at patchen tar nyeste-plassen i vedtaksdato-sorteringen.
     * Saker som kaprer uten å stå her avvises, fordi de bytter hvilken behandling avgiftsgrunnlaget
     * hentes fra. Liste og ikke flagg: ett kapret saksnummer skal ikke tvinge deg til å slå av selen
     * for de øvrige sakene i kallet.
     */
    val tillatSorteringsendring: List<String> = emptyList(),
)

data class VedtaksmetadataAngreRequest(
    /** Tom liste = alle rader merket MELOSYS-8174-PATCH, uansett sak. Krever [bekreftAlle] ved skarp. */
    val saksnummer: List<String> = emptyList(),
    val skarp: Boolean = false,
    /**
     * Kvitterer ut en skarp kjøring uten scope. Uten denne avvises tomt scope, fordi et glemt
     * `saksnummer` ellers ruller tilbake alle patch-rader i basen — også fra tidligere kjøringer.
     */
    val bekreftAlle: Boolean = false,
)
