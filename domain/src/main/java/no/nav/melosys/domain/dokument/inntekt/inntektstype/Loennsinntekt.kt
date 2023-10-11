package no.nav.melosys.domain.dokument.inntekt.inntektstype

import com.fasterxml.jackson.annotation.JsonView
import no.nav.melosys.domain.dokument.DokumentView
import no.nav.melosys.domain.dokument.inntekt.Inntekt
import java.math.BigDecimal
import java.time.YearMonth

class Loennsinntekt(
    beloep: BigDecimal,
    fordel: String,
    inntektskilde: String,
    inntektsperiodetype: String,
    inntektsstatus: String,
    utbetaltIPeriode: YearMonth,

    @JsonView(DokumentView.Database::class)
    var antall: Int? = null
) : Inntekt(
    beloep = beloep,
    fordel = fordel,
    inntektskilde = inntektskilde,
    inntektsperiodetype = inntektsperiodetype,
    inntektsstatus = inntektsstatus,
    utbetaltIPeriode = utbetaltIPeriode
)
