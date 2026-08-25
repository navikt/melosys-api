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
        description = "Teller ferdigbehandlede behandlinger der rammeavtale om fjernarbeid er huket av og lovvalget " +
            "er fastsatt, totalt og fordelt på vedtaksår. Valgfri periode med fom/tom mot vedtaksdato " +
            "(tom er inklusiv). Med inkluderSaksnummer=true (standard) listes også Melosys saksnummer (MEL-nr) " +
            "per behandling, for sporbarhet ved spørsmål i enkeltsaker. Samme saksnummer kan forekomme flere " +
            "ganger dersom én sak har flere behandlinger med rammeavtalen huket av og eget vedtak — antallet " +
            "teller behandlinger, ikke saker. Sett inkluderSaksnummer=false for kun oversiktstallene.",
    )
    fun hentRammeavtaleFjernarbeidStatistikk(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") fom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") tom: LocalDate?,
        @RequestParam(required = false, defaultValue = "true") inkluderSaksnummer: Boolean,
    ): ResponseEntity<RammeavtaleFjernarbeidStatistikk> {
        log.info(
            "Henter statistikk for rammeavtale om fjernarbeid (fom={}, tom={}, inkluderSaksnummer={})",
            fom,
            tom,
            inkluderSaksnummer,
        )
        return ResponseEntity.ok(
            rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(fom, tom, inkluderSaksnummer),
        )
    }
}
