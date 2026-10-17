package no.nav.melosys.service.vedtak.etterregistrering

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import mu.KotlinLogging
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Protected
@RestController
@RequestMapping("/admin/vedtaksmeldinger/etterregistrering")
class EtterregistreringAvVedtaksmeldingerController(
    private val etterregistrering: EtterregistreringAvVedtaksmeldinger,
) {

    @Operation(
        summary = "Send vedtaksmeldingen på nytt til melosys-skattehendelser (simulering eller skarp kjøring)",
        description =
            "Finner avsluttede FTRL-behandlinger med en innvilget medlemskapsperiode som slutter på eller etter " +
            "`periodeTomFra`, og sender den samme vedtaksmeldingen som steget SEND_MELDING_OM_VEDTAK, i " +
            "vedtaksrekkefølge. Utvalget er det samme som Q-14 i melosys-kode-wiki. Uten `skarp` slås " +
            "identen opp og meldingen bygges, men ingenting sendes. melosys-skattehendelser hopper over " +
            "perioder personen har fra før, så et nytt kall på samme utvalg er trygt.\n\n" +
            "Følg kjøringen i `/status`, og se tallene per vedtaksmåned i `/rapport`.\n\n" +
            "### Felter\n" +
            "- `periodeTomFra`: standard 2024-01-01.\n" +
            "- `vedtakFra`, `vedtakTil`: deler utvalget i porsjoner etter vedtaksdato, ellers datoen " +
            "iverksettingen startet. Begge er med. Behandlinger uten dato er utenfor når en av dem er satt.\n" +
            "- `behandlingIder`: begrenser utvalget til disse behandlingene.\n" +
            "- `maksAntall`: stopper etter så mange sendte meldinger. Kreves for skarp kjøring.\n" +
            "- `maksFeil`: stopper etter så mange feil. Standard 100.\n\n" +
            "### Før du kjører\n" +
            "- Appen har to podder, og status og rapport ligger i minnet på poden som fikk kallet. Kjør mot " +
            "én pod med port-forward, og sjekk `pod` i `/status`.\n" +
            "- Kjøringen kan ligge i kø bak annet arbeid, og da er `isRunning` false. Send `/run` én gang, og " +
            "sjekk `/status` før du sender et nytt.",
    )
    @PostMapping("/run")
    fun run(@RequestBody request: EtterregistreringRequest): ResponseEntity<Map<String, Any?>> {
        request.valideringsfeil()?.let { feil ->
            return ResponseEntity.badRequest().body(mapOf("feil" to feil))
        }
        if (etterregistrering.status()["isRunning"] == true) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                mapOf("feil" to "En kjøring pågår allerede — se /status, og vent til isRunning er false")
            )
        }

        val parametre = request.tilParametre()
        log.info { "Starter etterregistrering av vedtaksmeldinger: $parametre" }
        etterregistrering.kjørAsynkront(parametre)

        return ResponseEntity.ok(
            mapOf(
                "melding" to if (request.skarp) "Skarp kjøring startet" else "Simulering startet",
                "skarp" to request.skarp,
                "maksAntall" to request.maksAntall,
                "statusEndpoint" to "/admin/vedtaksmeldinger/etterregistrering/status",
                "rapportEndpoint" to "/admin/vedtaksmeldinger/etterregistrering/rapport",
            )
        )
    }

    @Operation(summary = "Hent status for pågående eller siste kjøring")
    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any?>> = ResponseEntity.ok(etterregistrering.status())

    @Operation(
        summary = "Hent rapport for siste kjøring",
        description = "Tall per vedtaksmåned, behandlingene som ble sendt (i simulering: ville blitt sendt) og " +
            "behandlingene som feilet, med feiltype. Rapporten har ikke fødselsnummer.",
    )
    @GetMapping("/rapport")
    fun rapport(): ResponseEntity<EtterregistreringRapport> = ResponseEntity.ok(etterregistrering.rapport())
}

data class EtterregistreringRequest(
    @field:Schema(defaultValue = "2024-01-01")
    val periodeTomFra: LocalDate = LocalDate.of(2024, 1, 1),
    val vedtakFra: LocalDate? = null,
    val vedtakTil: LocalDate? = null,
    val behandlingIder: List<Long>? = null,
    val maksAntall: Int? = null,
    @field:Schema(defaultValue = "100")
    val maksFeil: Int = 100,
    @field:Schema(defaultValue = "false")
    val skarp: Boolean = false,
) {
    fun valideringsfeil(): String? = when {
        // Løkka håndhever bare taket når det er satt, så uten denne sjekken sender en skarp kjøring hele utvalget.
        skarp && (maksAntall == null || maksAntall <= 0) -> "Skarp kjøring krever et positivt maksAntall"
        maksAntall != null && maksAntall <= 0 -> "maksAntall må være positivt"
        maksFeil < 0 -> "maksFeil kan ikke være negativ"
        vedtakFra != null && vedtakTil != null && vedtakFra.isAfter(vedtakTil) -> "vedtakFra er etter vedtakTil"
        behandlingIder != null && behandlingIder.isEmpty() -> "behandlingIder må ha minst én id"
        else -> null
    }

    fun tilParametre() = EtterregistreringParametre(
        periodeTomFra = periodeTomFra,
        vedtakFra = vedtakFra,
        vedtakTil = vedtakTil,
        behandlingIder = behandlingIder?.toSet(),
        maksAntall = maksAntall,
        maksFeil = maksFeil,
        skarp = skarp,
    )
}
