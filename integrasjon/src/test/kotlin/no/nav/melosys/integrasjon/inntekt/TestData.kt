package no.nav.melosys.integrasjon.inntekt

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

