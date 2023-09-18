package no.nav.melosys.integrasjon.inntk

import io.getunleash.Unleash
import no.nav.melosys.domain.Saksopplysning
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
@Primary // Dette er kun i bruk nå som vi har toggle for å tvinge denne til å brukes
class InntektToggletService(
    private var unleash: Unleash,
    private val inntektService: InntektService,
    private val inntektRestService: InntektRestService
) : InntektFasade {

    override fun hentInntektListe(personID: String, fom: YearMonth, tom: YearMonth): Saksopplysning {
        if (unleash.isEnabled("melosys.rest.inntekt")) {
            return inntektRestService.hentInntektListe(personID, fom, tom)
        }
        return inntektService.hentInntektListe(personID, fom, tom)
    }
}
