package no.nav.melosys.tjenester.gui.fagsaker.aktoerer.historikk

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.service.aktoer.AktoerHistorikkService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@RequestMapping("/fagsaker")
@Api(tags = ["fagsaker"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class AktoerHistorikkTjeneste(
    private val aksesskontroll: Aksesskontroll,
    private val aktoerHistorikkService: AktoerHistorikkService,
    private val fagsakService: FagsakService
) {
    @GetMapping("/{saksnummer}/aktoerer/{rolle}/historikk")
    @ApiOperation(
        value = "Henter aktørhistorikk knyttet til et gitt saksnummer.",
        response = AktoerHistorikkDto::class,
        responseContainer = "List"
    )
    fun hentAktørHistorikk(
        @PathVariable("saksnummer") saksnummer: String,
        @PathVariable("rolle") rolle: Aktoersroller,
    ): List<AktoerHistorikkDto> {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        aksesskontroll.autoriserSakstilgang(saksnummer)

        val aktører = aktoerHistorikkService.hentAktørHistorikk(fagsak, rolle)

        return aktører.map {
            AktoerHistorikkDto(
                registrertFra = it.registrertFra,
                registretTil = it.registretTil,
                rolle = it.rolle,
                aktoerID = it.aktørId,
                personIdent = it.personIdent,
                institusjonsID = it.institusjonID,
                orgnr = it.orgnr,
                fullmakter = it.fullmakter,
            )
        }.sortedBy { it.registrertFra }.toList()
    }
}
