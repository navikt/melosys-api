package no.nav.melosys.tjenester.gui.brev

import io.getunleash.Unleash
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.placeholder.PlaceholderService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.placeholder.PlaceholderKatalogDto
import no.nav.melosys.tjenester.gui.dto.placeholder.PlaceholderVerdierDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

// To baser (katalog uten sakskontekst, verdier per behandling) – derfor fulle stier på metodenivå.
@Protected
@RestController
@Tag(name = "placeholdere")
class PlaceholderController(
    private val placeholderService: PlaceholderService,
    private val aksesskontroll: Aksesskontroll,
    private val unleash: Unleash,
) {

    @GetMapping("/brev/placeholdere")
    @Operation(summary = "Henter katalogen over placeholdere til brev")
    fun hentKatalog(): ResponseEntity<PlaceholderKatalogDto> {
        sjekkTilgang()
        return ResponseEntity.ok(PlaceholderKatalogDto.av(placeholderService.hentKatalog()))
    }

    @GetMapping("/behandlinger/{behandlingID}/placeholdere")
    @Operation(summary = "Henter placeholder-verdier for en behandling")
    fun hentVerdier(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<PlaceholderVerdierDto> {
        sjekkTilgang()
        aksesskontroll.autoriser(behandlingID)
        return ResponseEntity.ok(PlaceholderVerdierDto.av(placeholderService.hentVerdier(behandlingID)))
    }

    /**
     * Krever både melosys.tekstblokker og melosys.tekstblokker.dynamisk-placeholder.
     * Returnerer 404 når en av dem er av: endepunktet finnes ikke for denne brukeren.
     */
    private fun sjekkTilgang() {
        if (!unleash.isEnabled(ToggleName.MELOSYS_TEKSTBLOKKER) ||
            !unleash.isEnabled(ToggleName.MELOSYS_TEKSTBLOKKER_DYNAMISK_PLACEHOLDER)
        ) {
            throw IkkeFunnetException("Dynamiske placeholdere er ikke aktivert")
        }
    }
}
