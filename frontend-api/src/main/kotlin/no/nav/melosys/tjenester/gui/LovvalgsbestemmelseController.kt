package no.nav.melosys.tjenester.gui

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.models.responses.ApiResponse
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
@Tag(name = "lovvalgsbestemmelser")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class LovvalgsbestemmelseController(
    private val lovvalgsbestemmelseSerivce: LovvalgsbestemmelseService,
) {
    @GetMapping
    @Operation(summary = "Henter lovvalgsbestemmelser")
    fun hentLovvalgsbestemmelser(
        @RequestParam(value = "sakstype") sakstype: Sakstyper,
        @RequestParam(value = "sakstema", required = false) sakstema: Sakstemaer?,
        @RequestParam(value = "behandlingstema") behandlingstema: Behandlingstema,
        @RequestParam(value = "land", required = false) land: Land_iso2?
    ): Set<LovvalgBestemmelse> {
        return lovvalgsbestemmelseSerivce.hentLovvalgsbestemmelser(sakstype, sakstema, behandlingstema, land)
    }
}
