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
            "antallHendelserProsessert kan være lavere enn antallInputHendelser også i en fullført " +
            "kjøring, fordi duplikater og ugyldig input er fjernet først. " +
            "Merk at et tak som kapper saker i den siste hendelsen ikke synes på hendelsestellingen — " +
            "les antallSakerHoppetOverPgaTak, som er der uansett om kjøringen ble avbrutt eller ikke. " +
            "Pågår det allerede en kjøring, avvises den nye med 409. To kall i samme øyeblikk kan " +
            "likevel begge få 200; det andre avvises da stille av jobben, så sjekk /status og vent til " +
            "isRunning er false før du sender /run på nytt. " +
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

        // Stopper det vanlige tilfellet: en kjøring har pågått en stund, og noen sender /run på nytt.
        // Uten denne submitteres en ny task, og er alle jobbtrådene opptatt, legger den seg i kø og
        // kjører hele lista skarpt om igjen når den første er ferdig — nye årsavregninger og nye brev
        // til de samme borgerne, siden dedupliseringen bare virker innenfor én kjøring.
        //
        // Den dekker ikke to kall i samme øyeblikk: isRunning blir først true når den asynkrone
        // tasken har begynt å kjøre. Da avvises den andre stille av compareAndSet inne i jobben, og
        // svaret her sier «startet» selv om ingenting startet. Vakten i jobben er den harde; denne er
        // for at den som kjører skal få vite det i det tilfellet som faktisk oppstår.
        if (skattepliktigeAarsavregningDryrunService.status()["isRunning"] == true) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                mapOf("feil" to "En kjøring pågår allerede — se /status, og vent til isRunning er false")
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
