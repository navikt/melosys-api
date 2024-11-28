package no.nav.melosys.tjenester.gui.ftrl.bestemmelser.avklartefakta

import io.swagger.annotations.Api
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Vertslandsavtale_bestemmelser
import no.nav.melosys.service.ftrl.bestemmelse.avklartefakta.AvklarteFaktaForBestemmelse
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Api(tags = ["ftrl", "bestemmelser", "avklarte fakta"])
class AvklarteFaktaForBestemmelseController(private val avklarteFaktaForBestemmelse: AvklarteFaktaForBestemmelse) {

    @GetMapping("/ftrl/bestemmelser/{bestemmelse}/avklartefakta")
    fun hentAvklarteFakta(
        @PathVariable bestemmelse: String,
        @RequestParam("behandlingID", required = true) behandlingID: Long
    ): ResponseEntity<AvklarteFaktaForBestemmelseDto> {
        val avklarteFaktaDtoList =
            avklarteFaktaForBestemmelse.hentAvklarteFakta(convert(bestemmelse), behandlingID).map { AvklarteFaktaDto(it.type, it.muligeFakta) }
        return ResponseEntity.ok(AvklarteFaktaForBestemmelseDto(avklarteFaktaDtoList))
    }

    private fun convert(source: String): Bestemmelse {
        require(source.isNotBlank()) { "No matching Bestemmelse found for value: $source" }

        return Folketrygdloven_kap2_bestemmelser.values().firstOrNull { it.name == source.uppercase() } ?: Vertslandsavtale_bestemmelser.values()
            .firstOrNull { it.name == source.uppercase() } ?: throw IllegalArgumentException("No matching Bestemmelse found for value: $source")
    }

    data class AvklarteFaktaForBestemmelseDto(val avklarteFakta: List<AvklarteFaktaDto>)

    data class AvklarteFaktaDto(val faktaType: Avklartefaktatyper, val muligeFakta: List<String>)
}
