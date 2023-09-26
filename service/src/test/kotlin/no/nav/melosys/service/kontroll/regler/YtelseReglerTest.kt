package no.nav.melosys.service.kontroll.regler

import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned
import no.nav.melosys.domain.dokument.inntekt.Inntekt
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class YtelseReglerTest {
    @Test
    fun utbetaltYtelserFraOffentligIPeriode_finnerIngenTreff_ingenNyAvklarteFakta() {
        val fom = LocalDate.now().minusYears(2)
        val tom = LocalDate.now().minusYears(1)
        Assertions.assertThat(
            YtelseRegler.utbetaltYtelserFraOffentligIPeriode(
                hentInntektDokument(false, fom),
                fom,
                tom
            )
        )
            .isFalse()
    }

    @Test
    fun utbetaltYtelserFraOffentligIPeriode_finnerTreff_nyAvklarteFakta() {
        val fom = LocalDate.now().minusYears(2)
        val tom = LocalDate.now().minusYears(1)
        Assertions.assertThat(
            YtelseRegler.utbetaltYtelserFraOffentligIPeriode(
                hentInntektDokument(
                    true,
                    fom.minusMonths(1)
                ), fom, tom
            )
        )
            .isTrue()
    }

    @Test
    fun utbetaltYtelserFraOffentligIPeriode_periodeFerdigIngenOffentligeYtelser_ingenNyAvklarteFakta() {
        val fom = LocalDate.now().minusYears(2)
        val tom = LocalDate.now().minusYears(1)
        Assertions.assertThat(
            YtelseRegler.utbetaltYtelserFraOffentligIPeriode(
                hentInntektDokument(
                    true,
                    fom.minusYears(4)
                ), fom, tom
            )
        )
            .isFalse()
    }

    @Test
    fun utbetaltYtelserFraOffentligIPeriode_tomTilDato_forespørTomTilDato() {
        val fom = LocalDate.now().minusYears(2)
        Assertions.assertThat(
            YtelseRegler.utbetaltYtelserFraOffentligIPeriode(
                hentInntektDokument(true, fom),
                fom,
                null
            )
        )
            .isTrue()
    }

    @Test
    fun utbetaltYtelserFraOffentligIPeriode_periodePåbegynt_verifiserInntektPeriode() {
        val fom = LocalDate.now().minusYears(1)
        val tom = LocalDate.now().plusYears(1)
        Assertions.assertThat(
            YtelseRegler.utbetaltYtelserFraOffentligIPeriode(
                hentInntektDokument(true, fom),
                fom,
                tom
            )
        )
            .isTrue()
    }

    @Test
    fun utbetaltYtelserFraOffentligIPeriode_periodeIkkePåbegynt_verifiserInntektPeriode() {
        val fom = LocalDate.now().plusYears(1)
        val tom = LocalDate.now().plusYears(2)
        Assertions.assertThat(
            YtelseRegler.utbetaltYtelserFraOffentligIPeriode(
                hentInntektDokument(true, fom),
                fom,
                tom
            )
        )
            .isTrue()
    }

    private fun hentInntektDokument(medYtelserFraOffentlig: Boolean, fom: LocalDate): InntektDokument {
        val inntektDokument = InntektDokument()
        inntektDokument.arbeidsInntektMaanedListe = ArrayList()
        inntektDokument.arbeidsInntektMaanedListe.add(hentArbeidsInntektMaaned(medYtelserFraOffentlig, fom))
        return inntektDokument
    }

    private fun hentArbeidsInntektMaaned(medYtelserFraOffentlig: Boolean, fom: LocalDate): ArbeidsInntektMaaned {
        val arbeidsInntektMaaned = ArbeidsInntektMaaned()
        arbeidsInntektMaaned.arbeidsInntektInformasjon = ArbeidsInntektInformasjon()
        arbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe = hentInntektsListe(medYtelserFraOffentlig, fom)
        return arbeidsInntektMaaned
    }

    private fun hentInntektsListe(medYtelserFraOffentlig: Boolean, fom: LocalDate): List<Inntekt> {
        val inntektsListe: MutableList<Inntekt> = ArrayList()
        inntektsListe.add(Inntekt())
        if (medYtelserFraOffentlig) {
            val ytelseFraOffentlige = YtelseFraOffentlige()
            ytelseFraOffentlige.utbetaltIPeriode = YearMonth.from(fom).plusMonths(1)
            inntektsListe.add(ytelseFraOffentlige)
        }
        return inntektsListe
    }
}
