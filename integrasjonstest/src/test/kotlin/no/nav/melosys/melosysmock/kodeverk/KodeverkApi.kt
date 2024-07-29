package no.nav.melosys.melosysmock.kodeverk

import no.nav.melosys.integrasjon.kodeverk.impl.dto.BeskrivelseDto
import no.nav.melosys.integrasjon.kodeverk.impl.dto.BetydningDto
import no.nav.melosys.integrasjon.kodeverk.impl.dto.FellesKodeverkDto
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/kodeverk/")
@Unprotected
class KodeverkApi {
    @GetMapping("{kodeverkNavn}/koder/betydninger")
    fun hent(@PathVariable kodeverkNavn: String): FellesKodeverkDto {
        return FellesKodeverkDto().apply {
            betydninger = mapOf("andreSkift" to listOf(BetydningDto().apply {
                gyldigFra = LocalDate.of(2019, 1, 1)
                gyldigTil = LocalDate.of(9999, 12, 31)
                beskrivelser = mapOf("nb" to BeskrivelseDto().apply {
                    term = "Andre skift"
                    tekst = "Andre skift"
                })
            }))
        }
    }
}

