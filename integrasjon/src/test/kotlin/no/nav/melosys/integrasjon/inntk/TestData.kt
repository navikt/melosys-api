package no.nav.melosys.integrasjon.inntk

import no.nav.melosys.integrasjon.inntk.inntekt.Aktoer
import no.nav.melosys.integrasjon.inntk.inntekt.AktoerType
import no.nav.melosys.integrasjon.inntk.inntekt.InntektResponse
import java.time.YearMonth

object TestData {
    val aktoer = Aktoer("123456789", AktoerType.AKTOER_ID)

    val avvik = InntektResponse.Avvik(
        ident = aktoer,
        opplysningspliktig = aktoer,
        virksomhet = aktoer,
        avvikPeriode = YearMonth.now(),
        tekst = "Default Text"
    )
}

