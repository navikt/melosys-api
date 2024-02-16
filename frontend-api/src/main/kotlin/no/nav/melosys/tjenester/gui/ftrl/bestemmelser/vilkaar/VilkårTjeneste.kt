package no.nav.melosys.tjenester.gui.ftrl.bestemmelser.vilkaar

import io.swagger.annotations.Api
import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.service.ftrl.bestemmelse.vilkaar.VilkårForBestemmelse
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@Api(tags = ["ftrl", "bestemmelser", "avklarte fakta", "vilkår"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class VilkårTjeneste(private val vilkårForBestemmelse: VilkårForBestemmelse) {
    private val log = KotlinLogging.logger { }

    @GetMapping("/ftrl/bestemmelser/{bestemmelse}/vilkaar")
    fun hentVilkår(
        @PathVariable bestemmelse: Folketrygdloven_kap2_bestemmelser,
        @RequestParam("behandlingID", required = true) behandlingID: Long,
        @RequestParam queryParams : Map<String, String>
    ): ResponseEntity<VilkårForBestemmelseDto> {
        val validKeys = Avklartefaktatyper.values().map { it.name }
        if (queryParams.keys.filterNot { it.uppercase() in validKeys }.isNotEmpty()) {
            throw RuntimeException("")
        }

        log.info { "FTRL vilkår: $bestemmelse, $behandlingID, $queryParams" }

        val vilkårDtoList =
            vilkårForBestemmelse.hentVilkår(bestemmelse, queryParams.mapKeys { (k, _) -> Avklartefaktatyper.valueOf(k) }, behandlingID)
                .map { VilkårOgBegrunnelserDto(it.vilkår, it.defaultOppfylt, it.muligeBegrunnelser) }
        return ResponseEntity.ok(VilkårForBestemmelseDto(vilkårDtoList))
    }

    data class VilkårForBestemmelseDto(val avklarteFakta: List<VilkårOgBegrunnelserDto>)

    data class VilkårOgBegrunnelserDto(val vilkår: Vilkaar, val defaultOppfylt: Boolean?, val muligeBegrunnelser: Collection<String>)
}
