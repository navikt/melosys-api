package no.nav.melosys.service.kontroll.regler

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned
import no.nav.melosys.domain.dokument.inntekt.Inntekt
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.inntekt.inntektstype.Loennsinntekt
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class YtelseReglerTest {
    @Test
    fun utbetaltYtelserFraOffentligIPeriode_finnerIngenTreff_ingenNyAvklarteFakta() {
        val fom = LocalDate.now().minusYears(2)
        val tom = LocalDate.now().minusYears(1)
        YtelseRegler.utbetaltYtelserFraOffentligIPeriode(
            hentInntektDokument(false, fom),
            fom,
            tom
        ).shouldBeFalse()
    }

    @Test
    fun utbetaltYtelserFraOffentligIPeriode_finnerTreff_nyAvklarteFakta() {
        val fom = LocalDate.now().minusYears(2)
        val tom = LocalDate.now().minusYears(1)
        YtelseRegler.utbetaltYtelserFraOffentligIPeriode(
            hentInntektDokument(true, fom.minusMonths(1)),
            fom,
            tom
        ).shouldBeTrue()
    }

    @Test
    fun utbetaltYtelserFraOffentligIPeriode_periodeFerdigIngenOffentligeYtelser_ingenNyAvklarteFakta() {
        val fom = LocalDate.now().minusYears(2)
        val tom = LocalDate.now().minusYears(1)
        YtelseRegler.utbetaltYtelserFraOffentligIPeriode(
            hentInntektDokument(true, fom.minusYears(4)),
            fom,
            tom
        ).shouldBeFalse()
    }

    @Test
    fun utbetaltYtelserFraOffentligIPeriode_tomTilDato_forespørTomTilDato() {
        val fom = LocalDate.now().minusYears(2)
        YtelseRegler.utbetaltYtelserFraOffentligIPeriode(
            hentInntektDokument(true, fom),
            fom,
            null
        ).shouldBeTrue()
    }

    @Test
    fun utbetaltYtelserFraOffentligIPeriode_periodePåbegynt_verifiserInntektPeriode() {
        val fom = LocalDate.now().minusYears(1)
        val tom = LocalDate.now().plusYears(1)
        YtelseRegler.utbetaltYtelserFraOffentligIPeriode(
            hentInntektDokument(true, fom),
            fom,
            tom
        ).shouldBeTrue()
    }

    @Test
    fun utbetaltYtelserFraOffentligIPeriode_periodeIkkePåbegynt_verifiserInntektPeriode() {
        val fom = LocalDate.now().plusYears(1)
        val tom = LocalDate.now().plusYears(2)
        YtelseRegler.utbetaltYtelserFraOffentligIPeriode(
            hentInntektDokument(true, fom),
            fom,
            tom
        ).shouldBeTrue()
    }

    @Test
    fun `utbetaltYtelserFraOffentligIPeriode ikke kast null pointer exception når inntektListe er null`() {
        val fom = LocalDate.now().plusYears(1)
        val tom = LocalDate.now().plusYears(2)
        YtelseRegler.utbetaltYtelserFraOffentligIPeriode(
            InntektDokument().apply {
                arbeidsInntektMaanedListe = listOf(ArbeidsInntektMaaned(arbeidsInntektInformasjon = ArbeidsInntektInformasjon()))
            },
            fom,
            tom
        ).shouldBeFalse()
    }

    private fun hentInntektDokument(medYtelserFraOffentlig: Boolean, fom: LocalDate): InntektDokument = InntektDokument().apply {
        arbeidsInntektMaanedListe = listOf(hentArbeidsInntektMaaned(medYtelserFraOffentlig, fom))

    }

    private fun hentArbeidsInntektMaaned(medYtelserFraOffentlig: Boolean, fom: LocalDate): ArbeidsInntektMaaned = ArbeidsInntektMaaned(
        arbeidsInntektInformasjon = ArbeidsInntektInformasjon()).apply {
        arbeidsInntektInformasjon.inntektListe = hentInntektsListe(medYtelserFraOffentlig, fom)
    }

    private fun hentInntektsListe(medYtelserFraOffentlig: Boolean, fom: LocalDate): List<Inntekt> =
        listOf(Loennsinntekt()) + if (medYtelserFraOffentlig) listOf(YtelseFraOffentlige().apply {
            utbetaltIPeriode = YearMonth.from(fom).plusMonths(1)
        }) else emptyList()
}
