package no.nav.melosys.tjenester.gui.aarsavregning

import io.swagger.annotations.Api
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Protected
@RestController
@Api(tags = ["årsavregning", "trygdeavgift"])
@RequestMapping("/fagsaker/{saksnummer}/aarsavregninger")
class ÅrsavregningListController(
    private val årsavregningService: ÅrsavregningService,
    private val aksesskontroll: Aksesskontroll,
) {


    @GetMapping
    fun hentAvregninger(
        @PathVariable("saksnummer") saksnummer: String,
        @RequestParam("aar") aar: Int?,
        @RequestParam("behandlingstype") behandlingstype: Behandlingsresultattyper?
    ): ResponseEntity<List<ÅrsavregningListResponse>> {
        aksesskontroll.autoriserSakstilgang(saksnummer)

        val filtrerteÅrsavregninger = årsavregningService.finnÅrsavregningerPåFagsak(saksnummer, aar, behandlingstype)
        val årsavregningListResponse = filtrerteÅrsavregninger.map {
            ÅrsavregningListResponse(
                aarsavregningId = it.id,
                behandlingID = it.behandlingsresultat.behandling.id,
                aar = it.aar,
                type = it.behandlingsresultat.type
            )
        }.toList()
        return ResponseEntity.ok(årsavregningListResponse)
    }

}
