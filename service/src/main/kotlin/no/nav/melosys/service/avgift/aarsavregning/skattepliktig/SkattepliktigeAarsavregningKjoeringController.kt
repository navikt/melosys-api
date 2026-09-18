package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
        description = RUN_BESKRIVELSE,
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
        // Sammen med en liste ville filtrene blitt ignorert, mens svaret bekreftet dem.
        if (request.gjelderAar == null && (request.aarFilter != null || request.publisertEtter != null)) {
            return ResponseEntity.badRequest().body(
                mapOf("feil" to "aarFilter og publisertEtter gjelder bare sammen med gjelderAar")
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

        // Fanger bare et nytt kall mens en kjøring pågår. Ligger den første fortsatt i kø, er isRunning
        // false, og begge kjøres etter hverandre på den ene jobbtråden.
        if (kjoering.status()["isRunning"] == true) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                mapOf("feil" to "En kjøring pågår allerede — se /status, og vent til isRunning er false")
            )
        }

        val modus = if (request.skarp) "SKARP" else "DRYRUN"
        val gjelderÅr = request.gjelderAar
        val årFilter = request.aarFilter ?: ÅrFilter.FOM_AAR
        if (gjelderÅr != null) {
            log.info {
                "Starter $modus for skattehendelser fra melosys-skattehendelser: gjelderÅr=$gjelderÅr, " +
                    "årFilter=$årFilter, publisertEtter=${request.publisertEtter}, maksAntall=${request.maksAntall}, " +
                    "hoppOverSakerMedAarsavregning=${request.hoppOverSakerMedAarsavregning}"
            }
            kjoering.prosesserSkattepliktigeFraSkattehendelserAsynkront(
                gjelderÅr,
                årFilter,
                request.publisertEtter,
                request.skarp,
                request.maksAntall,
                request.hoppOverSakerMedAarsavregning,
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
                "aarFilter" to if (gjelderÅr != null) årFilter else null,
                "publisertEtter" to request.publisertEtter,
                "statusEndpoint" to "/admin/aarsavregninger/saker/skattepliktige/status",
                "rapportEndpoint" to "/admin/aarsavregninger/saker/skattepliktige/rapport"
            )
        )
    }

    @Operation(summary = "Hent status for pågående eller siste kjøring")
    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any?>> =
        ResponseEntity(kjoering.status(), HttpStatus.OK)

    @Operation(summary = "Hent rapport med alle sakene fra siste kjøring")
    @GetMapping("/rapport", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun rapport(): ResponseEntity<String> =
        ResponseEntity(kjoering.rapportJsonString(), HttpStatus.OK)
}

data class SkattehendelseRunRequest(
    val skattehendelser: List<SkattehendelseItem> = emptyList(),
    val gjelderAar: Int? = null,
    val aarFilter: ÅrFilter? = null,
    val publisertEtter: LocalDateTime? = null,
    val skarp: Boolean = false,
    val maksAntall: Int? = null,
    val hoppOverSakerMedAarsavregning: Boolean = false,
)

private const val RUN_BESKRIVELSE = """
Kjører skattehendelser på nytt med de samme vurderingene som Kafka-flyten gjør løpende.
Uten `skarp` endres ingenting, og rapporten viser hva kjøringen ville gjort. Med `skarp=true`
opprettes årsavregninger, og åpne årsavregninger settes til VURDER_DOKUMENT.

Følg kjøringen i `/status`, og se resultatet per sak i `/rapport`. `avbruttAarsak` er satt hvis
kjøringen stoppet før den var ferdig.

**Felter**
- `skattehendelser` eller `gjelderAar`: send én av dem. Med `gjelderAar` hentes hendelsene fra
  melosys-skattehendelser. Hendelser for samme person og år slås sammen.
- `aarFilter` og `publisertEtter`: avgrenser hentingen, og gjelder bare sammen med `gjelderAar`.
  `aarFilter` er `FOM_AAR` (året perioden starter i, standard) eller `INNTEKTSAAR`.
  `publisertEtter` er norsk tid.
- `maksAntall`: påkrevd med `skarp=true`. Taket på hvor mange saker som kan endres. Saker som
  feiler før de er vurdert, og saker som hoppes over med `hoppOverSakerMedAarsavregning`,
  bruker ikke av taket.
- `hoppOverSakerMedAarsavregning`: hopper over saker som har en årsavregning for året, uansett
  status. Påkrevd med `skarp=true` og `gjelderAar`.

**Før du kjører**
- Send `/run` én gang. Kjøringen kan ligge i kø bak annet arbeid, og da er `isRunning` false.
  Et nytt kall i den tiden kjører alt to ganger og sender brevene på nytt. Sjekk `/rapport`
  for å se om kjøringen har startet.
- Appen har to podder, og status ligger i minnet på poden som fikk kallet. Kjør mot én pod med
  port-forward, og sjekk `pod` i `/status`.
- Vent til prosessinstansene fra forrige kjøring er ferdige før du starter en ny.
- Saker i `antallStatusHoppetOver` ble flyttet av en saksbehandler under kjøringen. Vurder dem
  manuelt: en ny kjøring setter dem tilbake til VURDER_DOKUMENT.
- Hele kjøringen holdes i minnet. Send lister i porsjoner på noen tusen hendelser, og avgrens
  `gjelderAar` med `publisertEtter`.
"""
