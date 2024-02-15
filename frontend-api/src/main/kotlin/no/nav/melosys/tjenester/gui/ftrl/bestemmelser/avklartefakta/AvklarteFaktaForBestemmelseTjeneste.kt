package no.nav.melosys.tjenester.gui.ftrl.bestemmelser.avklartefakta

import io.swagger.annotations.Api
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.service.ftrl.bestemmelse.avklartefakta.AvklarteFaktaForBestemmelse
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
@Api(tags = ["ftrl", "bestemmelser", "avklarte fakta"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class AvklarteFaktaForBestemmelseTjeneste(private val avklarteFaktaForBestemmelse: AvklarteFaktaForBestemmelse) {

    @GetMapping("/ftrl/bestemmelser/{bestemmelseID}/avklartefakta/")
    fun hentAvklarteFakta(
        @PathVariable bestemmelseID: Folketrygdloven_kap2_bestemmelser,
        @RequestParam("behandlingID", required = true) behandlingID: Long
    ): ResponseEntity<AvklarteFaktaForBestemmelseDto> {
        val avklarteFaktaDtoList =
            avklarteFaktaForBestemmelse.hentAvklarteFakta(bestemmelseID, behandlingID).map { AvklarteFaktaDto(it.type, it.muligeFakta) }
        return ResponseEntity.ok(AvklarteFaktaForBestemmelseDto(avklarteFaktaDtoList))
    }

    data class AvklarteFaktaForBestemmelseDto(val avklarteFakta: List<AvklarteFaktaDto>)

    data class AvklarteFaktaDto(val faktaType: Avklartefaktatyper, val muligeFakta: List<String>)
}
