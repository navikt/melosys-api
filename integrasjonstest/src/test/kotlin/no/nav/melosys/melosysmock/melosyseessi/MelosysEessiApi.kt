package no.nav.melosys.melosysmock.melosyseessi

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@Unprotected
class MelosysEessiApi {

    companion object SaksrelasjonLager {
        val saksrelasjoner = mutableSetOf<Saksrelasjon>()
    }

    @GetMapping("/buc/{bucType}/institusjoner")
    fun hentMottakerinstitusjoner() = emptyArray<Unit>()

    @GetMapping("/sak")
    fun hentSaksrelasjon(@RequestParam("rinaSaksnummer") rinaSaksnummer: String) = saksrelasjoner.filter { s -> s.rinaSaksnummer == rinaSaksnummer }

    @PostMapping("/sak")
    fun lagreSaksrelasjon(@RequestBody saksrelasjon: Saksrelasjon) {
        saksrelasjoner.add(saksrelasjon)
    }

}

data class Saksrelasjon(
    val gsakSaksnummer: Long,
    val rinaSaksnummer: String,
    val bucType: String
)
