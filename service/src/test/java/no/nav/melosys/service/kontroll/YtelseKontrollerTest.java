package no.nav.melosys.service.kontroll;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige;
import no.nav.melosys.domain.dokument.utbetaling.Utbetaling;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class YtelseKontrollerTest {

    @Test
    public void utbetaltYtelserFraOffentligIPeriode_finnerIngenTreff_ingenNyAvklarteFakta() {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        assertThat(YtelseKontroller.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(false, fom), fom, tom))
            .isFalse();
    }

    @Test
    public void utbetaltYtelserFraOffentligIPeriode_finnerTreff_nyAvklarteFakta() {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        assertThat(YtelseKontroller.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true, fom.minusMonths(1)), fom, tom))
            .isTrue();
    }

    @Test
    public void utbetaltYtelserFraOffentligIPeriode_periodeFerdigIngenOffentligeYtelser_ingenNyAvklarteFakta() {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        assertThat(YtelseKontroller.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true, fom.minusYears(4)), fom, tom))
            .isFalse();
    }

    @Test
    public void utbetaltYtelserFraOffentligIPeriode_tomTilDato_forespørTomTilDato() {

        LocalDate fom = LocalDate.now().minusYears(2);

        assertThat(YtelseKontroller.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true, fom), fom, null))
            .isTrue();
    }

    @Test
    public void utbetaltYtelserFraOffentligIPeriode_periodePåbegynt_verifiserInntektPeriode() {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);

        assertThat(YtelseKontroller.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true, fom), fom, tom))
            .isTrue();
    }

    @Test
    public void utbetaltYtelserFraOffentligIPeriode_periodeIkkePåbegynt_verifiserInntektPeriode() {
        LocalDate fom = LocalDate.now().plusYears(1);
        LocalDate tom = LocalDate.now().plusYears(2);

        assertThat(YtelseKontroller.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true, fom), fom, tom))
            .isTrue();
    }

    @Test
    public void utbetaltBarnetrygdytelserIPeriode_harIngenUtbetalingDokument_forventFalse() {
        assertThat(YtelseKontroller.utbetaltBarnetrygdytelserIPeriode(null)).isFalse();
    }

    @Test
    public void utbetaltBarnetrygdytelserIPeriode_harIngenUtbetalinger_forventFalse() {
        UtbetalingDokument utbetalingDokument = new UtbetalingDokument();
        utbetalingDokument.utbetalinger = Collections.emptyList();

        assertThat(YtelseKontroller.utbetaltBarnetrygdytelserIPeriode(utbetalingDokument)).isFalse();
        assertThat(YtelseKontroller.utbetaltBarnetrygdytelserIPeriode(new UtbetalingDokument())).isFalse();
    }

    @Test
    public void utbetaltBarnetrygdytelserIPeriode_harUtbetalinger_forventTrue() {
        UtbetalingDokument utbetalingDokument = new UtbetalingDokument();
        utbetalingDokument.utbetalinger = Arrays.asList(new Utbetaling(), new Utbetaling());

        assertThat(YtelseKontroller.utbetaltBarnetrygdytelserIPeriode(utbetalingDokument)).isTrue();
    }


    private InntektDokument hentInntektDokument(boolean medYtelserFraOffentlig, LocalDate fom) {
        InntektDokument inntektDokument = new InntektDokument();

        inntektDokument.arbeidsInntektMaanedListe = new ArrayList<>();
        inntektDokument.arbeidsInntektMaanedListe.add(hentArbeidsInntektMaaned(medYtelserFraOffentlig, fom));

        return inntektDokument;
    }

    private ArbeidsInntektMaaned hentArbeidsInntektMaaned(boolean medYtelserFraOffentlig, LocalDate fom) {
        ArbeidsInntektMaaned arbeidsInntektMaaned = new ArbeidsInntektMaaned();
        arbeidsInntektMaaned.arbeidsInntektInformasjon = new ArbeidsInntektInformasjon();
        arbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe = hentInntektsListe(medYtelserFraOffentlig, fom);

        return arbeidsInntektMaaned;
    }

    private List<Inntekt> hentInntektsListe(boolean medYtelserFraOffentlig, LocalDate fom) {
        List<Inntekt> inntektsListe = new ArrayList<>();
        inntektsListe.add(new Inntekt());

        if (medYtelserFraOffentlig) {
            YtelseFraOffentlige ytelseFraOffentlige = new YtelseFraOffentlige();
            ytelseFraOffentlige.utbetaltIPeriode = YearMonth.from(fom).plusMonths(1);
            inntektsListe.add(ytelseFraOffentlige);
        }

        return inntektsListe;
    }
}