package no.nav.melosys.tjenester.gui.kodeverk

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.nav.melosys.domain.kodeverk.Kodeverk
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_7_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.service.kodeverk.KodeDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext


@Protected
@RestController
@RequestMapping("/kodeverk/melosys-internt")
@Api(tags = ["kodeverk/melosys-internt"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class MelosysInterntKodeverkTjeneste {

    @GetMapping("/folketrygden")
    @ApiOperation(value = "Henter koder fra internt kodeverk til saksbehandling av folketrygden-saker")
    fun hentKoderTilFolketrygden(): ResponseEntity<Map<String, Map<String, Collection<KodeDto>>>> {
        val kodeverdier: MutableMap<String, Map<String, Collection<KodeDto>>> = HashMap()
        kodeverdier["begrunnelser"] = lagBegrunnelser()
        return ResponseEntity.ok(kodeverdier)
    }

    private fun lagBegrunnelser(): Map<String, Collection<KodeDto>> =
        mapOf(
            Ftrl_2_8_naer_tilknytning_norge_begrunnelser::class.java.getSimpleName() to
                mapKodeverk(*Ftrl_2_8_naer_tilknytning_norge_begrunnelser.values()),
            Ftrl_2_7_begrunnelser::class.java.getSimpleName() to
                mapKodeverk(*Ftrl_2_7_begrunnelser.values())
        )

    private fun mapKodeverk(vararg kodeverk: Kodeverk): Collection<KodeDto> =
        kodeverk.map { KodeDto(it.kode, it.beskrivelse) }
}

