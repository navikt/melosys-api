package no.nav.melosys.tjenester.gui.ftrl.bestemmelser.avklartefakta

import io.swagger.annotations.Api
import no.nav.melosys.domain.jpa.MedlemskapBestemmelsekonverter
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
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
            avklarteFaktaForBestemmelse.hentAvklarteFakta(medlemskapBestemmelsekonverter.convertToEntityAttribute(bestemmelse), behandlingID)
                .map { AvklarteFaktaDto(it.type, it.muligeFakta) }
        return ResponseEntity.ok(AvklarteFaktaForBestemmelseDto(avklarteFaktaDtoList))
    }

    data class AvklarteFaktaForBestemmelseDto(val avklarteFakta: List<AvklarteFaktaDto>)

    data class AvklarteFaktaDto(val faktaType: Avklartefaktatyper, val muligeFakta: List<String>)

    companion object {
        private val medlemskapBestemmelsekonverter = MedlemskapBestemmelsekonverter()
    }
}

