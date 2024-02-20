package no.nav.melosys.tjenester.gui.ftrl.bestemmelser.vilkaar

import io.swagger.annotations.Api
import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.ftrl.bestemmelse.vilkaar.VilkårForBestemmelse
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext

private const val BEHANDLING_ID = "behandlingID"

@Protected
@RestController
@Api(tags = ["ftrl", "bestemmelser", "avklarte fakta", "vilkår"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class VilkårTjeneste(private val vilkårForBestemmelse: VilkårForBestemmelse) {
    private val log = KotlinLogging.logger { }

    private val avklartefaktatyperNavn = Avklartefaktatyper.values().map { it.name }

    @GetMapping("/ftrl/bestemmelser/{bestemmelse}/vilkaar")
    fun hentVilkår(
        @PathVariable bestemmelse: Folketrygdloven_kap2_bestemmelser,
        @RequestParam requestParams : Map<String, String>
    ): ResponseEntity<VilkårForBestemmelseDto> {
        validerRequestParams(requestParams)

        val vilkårDtoList = vilkårForBestemmelse.hentVilkår(
            bestemmelse,
            requestParams.filterKeys { k -> k in avklartefaktatyperNavn }
                .mapKeys { (k, _) -> Avklartefaktatyper.valueOf(k) },
            requestParams[BEHANDLING_ID]?.toLong()
        ).map { VilkårOgBegrunnelserDto(it.vilkår, it.defaultOppfylt, it.muligeBegrunnelser) }

        log.debug { "FTRL vilkår for bestemmelse: $bestemmelse, $requestParams: $vilkårDtoList" }
        return ResponseEntity.ok(VilkårForBestemmelseDto(vilkårDtoList))
    }

    private fun validerRequestParams(queryParams: Map<String, String>) {
        val validKeys = listOf(BEHANDLING_ID) + avklartefaktatyperNavn

        val unknownKeys = queryParams.keys.filterNot { key ->  validKeys.any { it.equals(key, ignoreCase = true) } }
        if (unknownKeys.isNotEmpty()) {
            throw FunksjonellException("Følgende request params støttes ikke: " + unknownKeys)
        }
    }

    data class VilkårForBestemmelseDto(val vilkår: List<VilkårOgBegrunnelserDto>)

    data class VilkårOgBegrunnelserDto(val vilkår: Vilkaar, val defaultOppfylt: Boolean?, val muligeBegrunnelser: Collection<String>)
}
