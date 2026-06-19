package no.nav.melosys.saksflyt.statistikk

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Protected
@RestController
@Tags(Tag(name = "statistikk"), Tag(name = "admin"))
@RequestMapping("/admin/statistikk")
class RammeavtaleStatistikkController(
    private val rammeavtaleStatistikkService: RammeavtaleStatistikkService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/rammeavtale-fjernarbeid")
    @Operation(
        summary = "Statistikk for rammeavtale om fjernarbeid (TWA)",
        description = "Teller behandlinger der rammeavtale om fjernarbeid er huket av, totalt og fordelt på år. " +
            "Valgfri periode med fom/tom (tom er inklusiv).",
    )
    fun hentRammeavtaleFjernarbeidStatistikk(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") fom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") tom: LocalDate?,
    ): ResponseEntity<RammeavtaleFjernarbeidStatistikk> {
        log.info("Henter statistikk for rammeavtale om fjernarbeid (fom={}, tom={})", fom, tom)
        return ResponseEntity.ok(rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(fom, tom))
    }
}
