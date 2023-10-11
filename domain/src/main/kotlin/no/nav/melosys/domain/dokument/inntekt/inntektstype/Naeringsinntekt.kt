package no.nav.melosys.domain.dokument.inntekt.inntektstype

import no.nav.melosys.domain.dokument.inntekt.Inntekt
import java.math.BigDecimal
import java.time.YearMonth

class Naeringsinntekt(
    beloep: BigDecimal,
    fordel: String,
    inntektskilde: String,
    inntektsperiodetype: String,
    inntektsstatus: String,
    utbetaltIPeriode: YearMonth
) : Inntekt(
    beloep = beloep,
    fordel = fordel,
    inntektskilde = inntektskilde,
    inntektsperiodetype = inntektsperiodetype,
    inntektsstatus = inntektsstatus,
    utbetaltIPeriode = utbetaltIPeriode
)
