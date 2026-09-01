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

@Protected
@RestController
@RequestMapping("/admin/aarsavregninger/saker/skattepliktige")
class SkattepliktigeAarsavregningDryrunController(
    private val skattepliktigeAarsavregningDryrunService: SkattepliktigeAarsavregningDryrunService,
) {

    @Operation(
        summary = "Kjør skattehendelser på nytt (simulering eller ekte kjøring)",
        description = "Går gjennom skattehendelsene med de samme vurderingene som Kafka-flyten gjør " +
            "løpende. Med skarp=false (default) endres ingenting — svaret viser hva kjøringen ville " +
            "gjort. Med skarp=true har den to virkninger: den oppretter årsavregninger, og den setter " +
            "status til VURDER_DOKUMENT på saker som allerede har en åpen årsavregning. " +
            "Statusen leses på nytt rett før skriving og settes bare hvis behandlingen fortsatt står " +
            "der kjøringen så den; har en saksbehandler flyttet den siden, hoppes saken over og " +
            "telles i antallStatusHoppetOver, med årsak per sak i rapporten. Slike saker må vurderes " +
            "manuelt — de skal IKKE bare kjøres om igjen, for neste kjøring observerer den nye " +
            "statusen, og da slår sjekken ikke inn og saken settes tilbake til VURDER_DOKUMENT. " +
            "Ekte kjøring krever et positivt maksAntall — uten tak avvises kallet. " +
            "Hendelser med samme identifikator og år slås sammen før kjøring (antallDuplikaterFjernet), " +
            "fordi to hendelser for samme sak og år ellers gir to årsavregninger og to brev. " +
            "Overlappende kjøringer har samme svakhet: vent til køen er tømt før neste kjøring. " +
            "Ble kjøringen avbrutt — av taket eller av for mange feil — sier avbruttAarsak hvorfor, og " +
            "antallHendelserProsessert mot antallUnikeHendelser viser hvor langt den kom. Merk at " +
            "antallHendelserProsessert er lavere enn antallInputHendelser også i en fullført kjøring, " +
            "fordi duplikater og ugyldig input er fjernet først. " +
            "Starter du en kjøring mens en annen pågår, avvises den med 409 — den forrige fortsetter. " +
            "Bruk /status for fremdrift og /rapport for resultat per sak. NB: appen kjører to podder, " +
            "og jobbtilstanden ligger i minnet på den poden som tok imot /run — kjør derfor mot én pod " +
            "(port-forward), og kryssjekk pod-feltet i /status. Hele kjøringen holder én lesetransaksjon " +
            "og én persistence-kontekst, så kjør i porsjoner på noen tusen hendelser."
    )
    @PostMapping("/run")
    fun run(
        @RequestBody
        @Parameter(description = "Liste med skattehendelser, skarp-flagg, og valgfritt maksAntall")
        request: SkattehendelseRunRequest
    ): ResponseEntity<Map<String, Any?>> {
        // Uten denne starter {"skarp": true} en kjøring helt uten tak, fordi løkka bare håndhever
        // taket når verdien ikke er null. En full kjøring sender bare et høyt tall — poenget er at
        // taket skal være et valg, ikke en default.
        if (request.skarp && (request.maksAntall == null || request.maksAntall <= 0)) {
            return ResponseEntity.badRequest().body(
                mapOf(
                    "feil" to "Ekte kjøring krever et positivt maksAntall — taket avgjør hvor mange saker som kan endres",
                    "maksAntall" to request.maksAntall
                )
            )
        }

        // Kjøringen er asynkron, så et 200-svar her ville ellers sagt «startet» også når jobben
        // avviser fordi en annen kjøring pågår — og /status ville vist den forrige kjøringens tall.
        // Den harde vakten ligger i jobben selv; dette er for at den som kjører skal få vite det.
        if (skattepliktigeAarsavregningDryrunService.status()["isRunning"] == true) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                mapOf("feil" to "En kjøring pågår allerede — se /status, og vent til den er ferdig")
            )
        }

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

    @Operation(summary = "Hent status for pågående eller siste kjøring")
    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any?>> =
        ResponseEntity(skattepliktigeAarsavregningDryrunService.status(), HttpStatus.OK)

    @Operation(summary = "Hent rapport med alle sakene fra siste kjøring")
    @GetMapping("/rapport", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun rapport(): ResponseEntity<String> =
        ResponseEntity(skattepliktigeAarsavregningDryrunService.rapportJsonString(), HttpStatus.OK)
}

data class SkattehendelseRunRequest(
    val skattehendelser: List<SkattehendelseDryrunItem>,
    val skarp: Boolean = false,
    /** Tak på antall saker som kan endres. Påkrevd og positiv når [skarp] er true; teller også forsøk som feiler eller hoppes over. */
    val maksAntall: Int? = null,
)
