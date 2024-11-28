package no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
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
@Api(tags = ["lovlige-kombinasjoner", "medlemskapsperiode"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class LovligeKombinasjonerMedlemskapsperiodeController {

    @GetMapping("/bestemmelse/lovlige-kombinasjoner")
    @ApiOperation(value = "Henter lovlige bestemmelser basert på trygdedekning")
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
    @ApiOperation(value = "Henter lovlige trygdedekninger basert på bestemmelse")
    fun hentLovligeTrygdedekninger(
        @RequestParam(
            "bestemmelse",
            required = true
        ) bestemmelse: Folketrygdloven_kap2_bestemmelser
    ): ResponseEntity<List<Trygdedekninger>> {
        val trygdedekninger = LovligeKombinasjonerTrygdedekningBestemmelse.hentLovligeTrygdedekninger(bestemmelse)

        return ResponseEntity.ok(trygdedekninger)
    }
}
