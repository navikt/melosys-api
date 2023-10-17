package no.nav.melosys.service.kontroll.feature.ufm.kontroll

import no.nav.melosys.domain.dokument.inntekt.Inntekt
import no.nav.melosys.domain.dokument.inntekt.InntektType
import java.math.BigDecimal
import java.time.YearMonth

object InntektTestFactory {
    @JvmStatic
    fun createInntektForTest(
        type: InntektType,
        yearMonth: YearMonth = YearMonth.now().plusYears(2)
    ): Inntekt {
        return Inntekt(
            type = type,
            beloep = BigDecimal(50000),
            fordel = "fordel",
            inntektskilde = "inntektskilde",
            inntektsperiodetype = "inntektsperiodetype",
            inntektsstatus = "inntektsstatus",
            utbetaltIPeriode = yearMonth
        )
    }
}
