package no.nav.melosys.melosysmock.inntekt

import no.nav.melosys.integrasjon.inntekt.Aktoer
import no.nav.melosys.integrasjon.inntekt.AktoerType
import no.nav.melosys.integrasjon.inntekt.InntektRequest
import no.nav.melosys.integrasjon.inntekt.InntektResponse
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/inntektskomponenten/rs/api/v1")
@Unprotected
class InntektRestApi {
    @PostMapping("/hentinntektliste")
    fun hentInntekt(@RequestBody inntektRequest: InntektRequest): InntektResponse {
        return InntektResponse(listOf(), Aktoer(inntektRequest.ident.identifikator, AktoerType.AKTOER_ID))
    }
}
