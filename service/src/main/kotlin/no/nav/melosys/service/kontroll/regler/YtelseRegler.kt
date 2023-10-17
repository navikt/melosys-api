package no.nav.melosys.service.kontroll.regler

import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige
import java.time.LocalDate
import java.time.YearMonth

object YtelseRegler {
    @JvmStatic
    fun utbetaltYtelserFraOffentligIPeriode(inntektDokument: InntektDokument?, fom: LocalDate?, tom: LocalDate?): Boolean {
        if (inntektDokument == null || inntektDokument.arbeidsInntektMaanedListe.isEmpty()) {
            return false
        }
        val fra = YearMonth.from(fom)
        val til = if (tom != null) YearMonth.from(tom) else null
        for (ytelseFraOffentlige in hentYtelseFraOffentlige(inntektDokument)) {
            if (erUtbetaltIPeriode(ytelseFraOffentlige, fra, til)) {
                return true
            }
        }
        return false
    }

    private fun erUtbetaltIPeriode(ytelseFraOffentlige: YtelseFraOffentlige, fom: YearMonth, tom: YearMonth?): Boolean {
        val utbetaltIPeriode = ytelseFraOffentlige.utbetaltIPeriode

        val tomNotNull = tom ?: fom.plusYears(2)
        if (utbetaltIPeriode.isAfter(fom) && utbetaltIPeriode.isBefore(tomNotNull))
            return true

        return utbetaltIPeriode == fom || utbetaltIPeriode == tom
    }

private fun hentYtelseFraOffentlige(inntektDokument: InntektDokument): Collection<YtelseFraOffentlige> =
    inntektDokument.arbeidsInntektMaanedListe
        .mapNotNull { it.arbeidsInntektInformasjon.inntektListe }
        .flatten()
        .filterIsInstance<YtelseFraOffentlige>()
        .toList()
}
