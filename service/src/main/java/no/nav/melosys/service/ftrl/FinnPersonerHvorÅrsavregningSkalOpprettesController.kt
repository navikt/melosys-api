package no.nav.melosys.service.ftrl

import mu.KotlinLogging
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@Unprotected
@RestController
@RequestMapping("/admin/ftrl/finn-personer-hvor-aarsavregning-skal-opprettes")
class FinnPersonerHvorÅrsavregningSkalOpprettesController(
    private val finnPersonerHvorÅrsavregningSkalOpprettes: FinnPersonerHvorÅrsavregningSkalOpprettes
)  {

    @PostMapping("")
    fun finnPersonerOgSendVedtakMeldinger(
        @RequestParam(required = false, defaultValue = "true") dryrun: Boolean,
    ): ResponseEntity<Unit> {
        log.info("finnPersonerOgSendVedtakMeldinger - dryrun $dryrun")

        finnPersonerHvorÅrsavregningSkalOpprettes.kjørFinnSakerOgLeggPåKøAsynkront(dryrun)

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity<Map<String, Any>>(mapOf(), HttpStatus.OK)
    }
}
