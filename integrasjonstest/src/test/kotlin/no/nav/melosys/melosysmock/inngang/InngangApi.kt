package no.nav.melosys.melosysmock.inngang

import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api")
@Unprotected
class InngangApi {


    @PostMapping("/inngangsvilkaar")
    fun inngangsVilkår(@RequestBody request: Request): InngangsvilkarResponse {
        return InngangsvilkarResponse().apply {
            kvalifisererForEf883_2004 = false
        }
    }

}

data class Request(
    val statsborgerskap: Set<String>? = null,
    val arbeidsland: Set<String>? = null,
    val erUkjenteEllerAlleEosLand: Boolean = false,
    val periode: ErPeriode? = null,
)

data class ErPeriode(
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
)
