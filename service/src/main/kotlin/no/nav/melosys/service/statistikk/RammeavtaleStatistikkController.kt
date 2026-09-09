package no.nav.melosys.service.statistikk

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import no.nav.melosys.sikkerhet.context.SubjectHandler
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
            "teller behandlinger, ikke saker. Sett inkluderSaksnummer=false for en response med kun " +
            "oversiktstallene (samme spørring, mindre response). NB: uttrekket dekker kun saker der Norge selv " +
            "har sendt anmodningen — innkommende A001 fra andre land er ikke med (MELOSYS-8252).",
    )
    fun hentRammeavtaleFjernarbeidStatistikk(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") fom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") tom: LocalDate?,
        @RequestParam(required = false, defaultValue = "true") inkluderSaksnummer: Boolean,
    ): ResponseEntity<RammeavtaleFjernarbeidStatistikk> {
        // Logges før og etter: uttrekket er upaginert, og en spørring som henger eller feiler er nettopp den
        // situasjonen sporet trengs i. Identen er med fordi dette er et uttrekk på saksnivå, ikke bare et tall
        val ident = SubjectHandler.getUserIDOrSystemUser()
        log.info(
            "{} henter statistikk for rammeavtale om fjernarbeid (fom={}, tom={}, inkluderSaksnummer={})",
            ident,
            fom,
            tom,
            inkluderSaksnummer,
        )
        val statistikk = rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(fom, tom, inkluderSaksnummer)
        log.info("{} hentet {} behandlinger med rammeavtale om fjernarbeid", ident, statistikk.antall)
        return ResponseEntity.ok(statistikk)
    }
}
