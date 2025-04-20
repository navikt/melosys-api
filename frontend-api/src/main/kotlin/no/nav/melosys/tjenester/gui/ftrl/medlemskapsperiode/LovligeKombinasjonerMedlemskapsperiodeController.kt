package no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import no.nav.melosys.domain.jpa.konverterTilBestemmelse
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.service.ftrl.bestemmelse.LovligeKombinasjonerTrygdedekningBestemmelse
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@RequestMapping("/medlemskapsperioder")
@Tags(
    Tag(name = "lovlige-kombinasjoner"),
    Tag(name = "medlemskapsperiode"),
)
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class LovligeKombinasjonerMedlemskapsperiodeController(
) {

    @GetMapping("/bestemmelse/lovlige-kombinasjoner")
    @Operation(summary = "Henter lovlige bestemmelser basert på trygdedekning")
    fun hentLovligeBestemmelser(
        @RequestParam(
            "trygdedekning",
            required = true
        ) trygdedekning: Trygdedekninger
    ): ResponseEntity<List<Bestemmelse>> {
        val bestemmelser = LovligeKombinasjonerTrygdedekningBestemmelse.hentLovligeBestemmelser(trygdedekning)

        return ResponseEntity.ok(bestemmelser)
    }

    @GetMapping("/trygdedekning/lovlige-kombinasjoner")
    @Operation(summary = "Henter lovlige trygdedekninger basert på bestemmelse")
    fun hentLovligeTrygdedekninger(
        @RequestParam(
            "bestemmelse",
            required = true
        ) bestemmelse: String
    ): ResponseEntity<List<Trygdedekninger>> {
        val trygdedekninger = LovligeKombinasjonerTrygdedekningBestemmelse.hentLovligeTrygdedekninger(
            konverterTilBestemmelse(bestemmelse)
        )

        return ResponseEntity.ok(trygdedekninger)
    }
}
