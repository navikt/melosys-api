package no.nav.melosys.tjenester.gui.aarsavregning

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Protected
@RestController
@Tags(
    Tag(name = "årsavregning"),
    Tag(name = "trygdeavgift")
)
@RequestMapping("/fagsaker/{saksnummer}/aarsavregninger")
class ÅrsavregningListController(
    private val årsavregningService: ÅrsavregningService,
    private val aksesskontroll: Aksesskontroll,
) {
    @GetMapping
    fun hentAvregninger(
        @PathVariable("saksnummer") saksnummer: String,
        @RequestParam("aar") aar: Int?,
        @RequestParam("resultattype") behandlingsresultattype: Behandlingsresultattyper?
    ): ResponseEntity<List<ÅrsavregningListResponse>> {
        aksesskontroll.autoriserSakstilgang(saksnummer)

        val filtrerteÅrsavregninger = årsavregningService.finnÅrsavregningerPåFagsak(saksnummer, aar, behandlingsresultattype)
        val årsavregningListResponse = filtrerteÅrsavregninger.map {
            ÅrsavregningListResponse(
                aarsavregningId = it.id,
                behandlingID = it.hentBehandlingsresultat.hentBehandling().id,
                aar = it.aar,
                resultattype = it.hentBehandlingsresultat.hentType()
            )
        }.toList()
        return ResponseEntity.ok(årsavregningListResponse)
    }

    data class ÅrsavregningListResponse(
        val aarsavregningId: Long,
        val behandlingID: Long,
        val aar: Int,
        val resultattype: Behandlingsresultattyper,
    )
}
