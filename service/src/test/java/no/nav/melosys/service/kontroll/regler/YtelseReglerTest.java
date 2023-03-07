package no.nav.melosys.service.kontroll.regler;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class YtelseReglerTest {

    @Test
    public void utbetaltYtelserFraOffentligIPeriode_finnerIngenTreff_ingenNyAvklarteFakta() {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        assertThat(YtelseRegler.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(false, fom), fom, tom))
            .isFalse();
    }

    @Test
    public void utbetaltYtelserFraOffentligIPeriode_finnerTreff_nyAvklarteFakta() {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        assertThat(YtelseRegler.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true, fom.minusMonths(1)), fom, tom))
            .isTrue();
    }

    @Test
    public void utbetaltYtelserFraOffentligIPeriode_periodeFerdigIngenOffentligeYtelser_ingenNyAvklarteFakta() {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        assertThat(YtelseRegler.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true, fom.minusYears(4)), fom, tom))
            .isFalse();
    }

    @Test
    public void utbetaltYtelserFraOffentligIPeriode_tomTilDato_forespørTomTilDato() {

        LocalDate fom = LocalDate.now().minusYears(2);

        assertThat(YtelseRegler.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true, fom), fom, null))
            .isTrue();
    }

    @Test
    public void utbetaltYtelserFraOffentligIPeriode_periodePåbegynt_verifiserInntektPeriode() {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);

        assertThat(YtelseRegler.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true, fom), fom, tom))
            .isTrue();
    }

    @Test
    public void utbetaltYtelserFraOffentligIPeriode_periodeIkkePåbegynt_verifiserInntektPeriode() {
        LocalDate fom = LocalDate.now().plusYears(1);
        LocalDate tom = LocalDate.now().plusYears(2);

        assertThat(YtelseRegler.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true, fom), fom, tom))
            .isTrue();
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
