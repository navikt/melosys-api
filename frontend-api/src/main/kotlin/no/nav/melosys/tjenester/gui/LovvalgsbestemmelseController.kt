package no.nav.melosys.tjenester.gui

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.service.lovvalgsbestemmelse.LovvalgsbestemmelseService
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@RequestMapping("/lovvalgsbestemmelser")
@Api(tags = ["lovvalgsbestemmelser"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class LovvalgsbestemmelseController(
    private val lovvalgsbestemmelseSerivce: LovvalgsbestemmelseService,
) {
    @GetMapping
    @ApiOperation(value = "Henter lovvalgsbestemmelser", response = LovvalgBestemmelse::class)
    fun hentLovvalgsbestemmelser(
        @RequestParam(value = "sakstype") sakstype: Sakstyper,
        @RequestParam(value = "sakstema", required = false) sakstema: Sakstemaer?,
        @RequestParam(value = "behandlingstema") behandlingstema: Behandlingstema,
        @RequestParam(value = "land", required = false) land: Land_iso2?
    ): Set<LovvalgBestemmelse> {
        return lovvalgsbestemmelseSerivce.hentLovvalgsbestemmelser(sakstype, sakstema, behandlingstema, land)
    }
}
