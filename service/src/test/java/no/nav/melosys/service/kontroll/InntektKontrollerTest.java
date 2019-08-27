package no.nav.melosys.service.kontroll;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InntektKontrollerTest {

    @Test
    public void utførSteg_finnerIngenTreff_ingenNyAvklarteFakta() throws Exception {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        assertThat(InntektKontroller.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(false, fom), fom, tom))
            .isFalse();
    }

    @Test
    public void utførSteg_finnerTreff_nyAvklarteFakta() throws Exception {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        assertThat(InntektKontroller.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true, fom.minusMonths(1)), fom ,tom))
            .isTrue();
    }

    @Test
    public void utførSteg_periodeFerdigIngenOffentligeYtelser_ingenNyAvklarteFakta() throws Exception {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        assertThat(InntektKontroller.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true,  fom.minusYears(4)), fom, tom))
            .isFalse();
    }

    @Test
    public void utførSteg_tomTilDato_forespørTomTilDato() throws Exception {

        LocalDate fom = LocalDate.now().minusYears(2);

        assertThat(InntektKontroller.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true, fom), fom, null))
            .isTrue();
    }

    @Test
    public void utførSteg_periodePåbegynt_verifiserInntektPeriode() throws Exception {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);

        assertThat(InntektKontroller.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true, fom), fom, tom))
            .isTrue();
    }

    @Test
    public void utførSteg_periodeIkkePåbegynt_verifiserInntektPeriode() throws Exception {
        LocalDate fom = LocalDate.now().plusYears(1);
        LocalDate tom = LocalDate.now().plusYears(2);

        assertThat(InntektKontroller.utbetaltYtelserFraOffentligIPeriode(hentInntektDokument(true, fom), fom, tom))
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