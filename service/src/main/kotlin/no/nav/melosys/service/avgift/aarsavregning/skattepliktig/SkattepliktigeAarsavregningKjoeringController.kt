package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import mu.KotlinLogging
import no.nav.melosys.integrasjon.skattehendelser.ÅrFilter
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

private val log = KotlinLogging.logger { }

@Protected
@RestController
@RequestMapping("/admin/aarsavregninger/saker/skattepliktige")
class SkattepliktigeAarsavregningKjoeringController(
    private val kjoering: SkattepliktigeAarsavregningKjoering,
) {

    @Operation(
        summary = "Opprett årsavregninger fra skattehendelser (simulering eller ekte kjøring)",
        description =
            "Kjører skattehendelser på nytt med de samme vurderingene som Kafka-flyten gjør løpende. Uten " +
            "`skarp` endres ingenting, og rapporten viser hva kjøringen ville gjort. Med `skarp=true` " +
            "opprettes årsavregninger. Åpne årsavregninger settes til VURDER_DOKUMENT bare når " +
            "`hoppOverSakerMedAarsavregning=false`.\n\n" +
            "Følg kjøringen i `/status`, og se resultatet per sak i `/rapport`. `avbruttAarsak` er satt hvis " +
            "kjøringen stoppet før den var ferdig.\n\n" +
            "### Felter\n" +
            "Ugyldige kombinasjoner avvises med 400 og en melding om hva som mangler.\n" +
            "- `skattehendelser` eller `gjelderAar`: med `gjelderAar` hentes hendelsene fra " +
            "melosys-skattehendelser. Hver sak vurderes bare én gang per år.\n" +
            "- `aarFilter`: `FOM_AAR` (året perioden starter i, standard) eller `INNTEKTSAAR`. " +
            "`publisertEtter` er norsk tid.\n" +
            "- `maksAntall`: taket på hvor mange saker som kan endres. Saker som hoppes over, eller feiler " +
            "før de er vurdert, teller ikke.\n" +
            "- `hoppOverSakerMedAarsavregning`: hopper over saker som har en årsavregning for året, også " +
            "avsluttede. Standard er `true`.\n" +
            "- `personIder`: `personId`-ene fra simuleringen du har gått gjennom. Id-er som ikke kom med i " +
            "hentingen, står i `personIderIkkeFunnet` i `/status`.\n\n" +
            "### Før du kjører\n" +
            "- Send `/run` én gang. Kjøringen kan ligge i kø bak annet arbeid, og da er `isRunning` false. Et " +
            "nytt kall i den tiden kjører alt to ganger og sender brevene på nytt. Sjekk `/rapport` for å se " +
            "om kjøringen har startet.\n" +
            "- Appen har to podder, og status ligger i minnet på poden som fikk kallet. Kjør mot én pod med " +
            "port-forward, og sjekk `pod` i `/status`.\n" +
            "- Vent til prosessinstansene fra forrige kjøring er ferdige før du starter en ny.\n" +
            "- Saker i `antallStatusHoppetOver` ble flyttet av en saksbehandler under kjøringen. Vurder dem " +
            "manuelt: en ny kjøring setter dem tilbake til VURDER_DOKUMENT.\n" +
            "- Hele kjøringen holdes i minnet. Send lister i porsjoner på noen tusen hendelser, og avgrens " +
            "`gjelderAar` med `publisertEtter`.",
    )
    @PostMapping("/run")
    fun run(
        @RequestBody
        @Parameter(description = "Enten skattehendelser eller gjelderAar, pluss valgene beskrevet over")
        request: SkattehendelseRunRequest
    ): ResponseEntity<Map<String, Any?>> {
        if (request.skattehendelser.isEmpty() == (request.gjelderAar == null)) {
            return ResponseEntity.badRequest().body(
                mapOf("feil" to "Send enten skattehendelser eller gjelderAar, ikke begge og ikke ingen av dem")
            )
        }
        // Sammen med en liste ville publisertEtter blitt ignorert, mens svaret bekreftet den.
        if (request.gjelderAar == null && request.publisertEtter != null) {
            return ResponseEntity.badRequest().body(
                mapOf("feil" to "publisertEtter gjelder bare sammen med gjelderAar")
            )
        }
        if (request.personIder != null && (request.gjelderAar == null || request.personIder.isEmpty())) {
            return ResponseEntity.badRequest().body(
                mapOf("feil" to "personIder gjelder bare sammen med gjelderAar, og må ha minst én id")
            )
        }
        // Uten lista ville en ekte kjøring også tatt personer publisert etter simuleringen.
        if (request.skarp && request.gjelderAar != null && request.personIder == null) {
            return ResponseEntity.badRequest().body(
                mapOf("feil" to "Ekte kjøring med gjelderAar krever personIder fra simuleringen")
            )
        }
        if (request.skarp && request.gjelderAar != null && !request.hoppOverSakerMedAarsavregning) {
            return ResponseEntity.badRequest().body(
                mapOf("feil" to "Ekte kjøring med gjelderAar krever hoppOverSakerMedAarsavregning")
            )
        }

        // Løkka håndhever bare taket når det er satt, så uten denne sjekken kjører {"skarp": true} uten tak.
        if (request.skarp && (request.maksAntall == null || request.maksAntall <= 0)) {
            return ResponseEntity.badRequest().body(
                mapOf(
                    "feil" to "Ekte kjøring krever et positivt maksAntall — taket avgjør hvor mange saker som kan endres",
                    "maksAntall" to request.maksAntall
                )
            )
        }

        // Fanger bare et nytt kall mens en kjøring pågår. Hvis den første fortsatt ligger i kø, er
        // isRunning false, og begge kjøres etter hverandre på den ene jobbtråden.
        if (kjoering.status()["isRunning"] == true) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                mapOf("feil" to "En kjøring pågår allerede — se /status, og vent til isRunning er false")
            )
        }

        val modus = if (request.skarp) "SKARP" else "DRYRUN"
        val gjelderÅr = request.gjelderAar
        if (gjelderÅr != null) {
            log.info {
                "Starter $modus for skattehendelser fra melosys-skattehendelser: gjelderAar=$gjelderÅr, " +
                    "aarFilter=${request.aarFilter}, publisertEtter=${request.publisertEtter}, maksAntall=${request.maksAntall}, " +
                    "hoppOverSakerMedAarsavregning=${request.hoppOverSakerMedAarsavregning}, " +
                    "antallPersonIder=${request.personIder?.size}"
            }
            kjoering.prosesserSkattepliktigeFraSkattehendelserAsynkront(
                gjelderÅr,
                request.aarFilter,
                request.publisertEtter,
                request.skarp,
                request.maksAntall,
                request.hoppOverSakerMedAarsavregning,
                request.personIder?.toSet(),
            )
        } else {
            log.info {
                "Starter $modus for ${request.skattehendelser.size} skattehendelser, maksAntall=${request.maksAntall}, " +
                    "hoppOverSakerMedAarsavregning=${request.hoppOverSakerMedAarsavregning}"
            }
            kjoering.prosesserSkattehendelserAsynkront(
                request.skattehendelser,
                request.skarp,
                request.maksAntall,
                request.hoppOverSakerMedAarsavregning,
            )
        }

        return ResponseEntity.ok(
            mapOf(
                "melding" to "$modus startet",
                "skarp" to request.skarp,
                "maksAntall" to request.maksAntall,
                "hoppOverSakerMedAarsavregning" to request.hoppOverSakerMedAarsavregning,
                "antallHendelser" to if (gjelderÅr != null) null else request.skattehendelser.size,
                "gjelderAar" to gjelderÅr,
                "aarFilter" to if (gjelderÅr != null) request.aarFilter else null,
                "publisertEtter" to request.publisertEtter,
                "antallPersonIder" to request.personIder?.size,
                "statusEndpoint" to "/admin/aarsavregninger/saker/skattepliktige/status",
                "rapportEndpoint" to "/admin/aarsavregninger/saker/skattepliktige/rapport"
            )
        )
    }

    @Operation(summary = "Hent status for pågående eller siste kjøring")
    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any?>> =
        ResponseEntity(kjoering.status(), HttpStatus.OK)

    @Operation(
        summary = "Hent rapport med alle sakene fra siste kjøring",
        description = "Rapporten har `personId` fra melosys-skattehendelser, ikke fødselsnummer. Slå opp personen " +
            "med `/admin/person/{id}` i melosys-skattehendelser.",
    )
    @GetMapping("/rapport", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun rapport(): ResponseEntity<String> =
        ResponseEntity(kjoering.rapportJsonString(), HttpStatus.OK)
}

data class SkattehendelseRunRequest(
    val skattehendelser: List<SkattehendelseItem> = emptyList(),
    val gjelderAar: Int? = null,
    @field:Schema(defaultValue = "FOM_AAR")
    val aarFilter: ÅrFilter = ÅrFilter.FOM_AAR,
    val publisertEtter: LocalDateTime? = null,
    @field:Schema(defaultValue = "false")
    val skarp: Boolean = false,
    val maksAntall: Int? = null,
    @field:Schema(defaultValue = "true")
    val hoppOverSakerMedAarsavregning: Boolean = true,
    val personIder: List<Long>? = null,
)
